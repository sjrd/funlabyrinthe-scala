package com.funlabyrinthe.editor.renderer

import scala.collection.mutable
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import scala.scalajs.js
import scala.scalajs.js.JSConverters.*

import java.io.IOException

import com.funlabyrinthe.coreinterface.{FunLabyInterface, GlobalEventHandler, Universe}

import com.funlabyrinthe.editor.common.FileService.ProjectLoadInfo
import com.funlabyrinthe.editor.renderer.electron.fileService
import com.funlabyrinthe.editor.renderer.model.ProjectDef

final class UniverseFile private (
  initProjectDef: ProjectDef,
  val intf: FunLabyInterface,
  initUniverseFileContent: String,
  isEditing: Boolean,
):
  import UniverseFile.*

  private var _universe: Option[Universe] = None

  val projectID = initProjectDef.id
  private val baseURI = File(initProjectDef.baseURI)

  val sourcesDirectory: File = baseURI / "sources"

  val sourceFiles: mutable.ArrayBuffer[String] = mutable.ArrayBuffer.empty
  var moduleClassNames: List[String] = Nil

  var onResourceLoaded: () => Unit = () => ()

  def universe: Universe =
    _universe.getOrElse {
      throw IllegalStateException(s"The universe is not ready yet")
    }

  private def createNew(): Future[this.type] =
    val defaultModules = List("com.funlabyrinthe.mazes.Mazes")
    moduleClassNames = defaultModules
    for universe <- intf.createNewUniverse(moduleClassNames.toJSArray, makeGlobalEventHandler()).toFuture yield
      _universe = Some(universe)
      this
  end createNew

  private def load(): Future[this.type] =
    unpickle(initProjectDef.projectFileContent)

    for universe <- intf.loadUniverse(moduleClassNames.toJSArray, initUniverseFileContent, makeGlobalEventHandler()).toFuture yield
      _universe = Some(universe)
      this
  end load

  private def makeGlobalEventHandler(): GlobalEventHandler = new {
    this.isEditing = UniverseFile.this.isEditing
    this.onResourceLoaded = () => UniverseFile.this.onResourceLoaded()
  }

  private def unpickle(projectFileContent: ProjectFileContent): Unit =
    moduleClassNames = projectFileContent.modules

    sourceFiles.clear()
    sourceFiles ++= projectFileContent.sources
  end unpickle

  def save(): Future[Unit] =
    val pickle = this.pickle()
    val pickleString = ProjectFileContent.stringifyProject(pickle)

    val universePickleString = universe.save()

    fileService.saveProject(projectID.id, pickleString, universePickleString).toFuture
  end save

  private def pickle(): ProjectFileContent =
    ProjectFileContent(
      modules = moduleClassNames,
      sources = sourceFiles.toList,
    )
  end pickle
end UniverseFile

object UniverseFile:
  def createNew(projectDef: ProjectDef, loadInfo: ProjectLoadInfo,
      globalResourcesDir: File): Future[UniverseFile] =
    for
      intf <- loadFunLabyInterface(loadInfo.runtimeURI)
      universeFile <- new UniverseFile(projectDef, intf, "", isEditing = false).createNew()
    yield
      universeFile
  end createNew

  def load(projectDef: ProjectDef, loadInfo: ProjectLoadInfo,
      globalResourcesDir: File, isEditing: Boolean): Future[UniverseFile] =
    for
      intf <- loadFunLabyInterface(loadInfo.runtimeURI)
      universeFile <- new UniverseFile(projectDef, intf, loadInfo.universeFileContent, isEditing).load()
    yield
      universeFile
  end load

  private def loadFunLabyInterface(runtimeURI: String): Future[FunLabyInterface] =
    js.`import`[FunLabyInterfaceModule](runtimeURI).toFuture.map(_.FunLabyInterface)
  end loadFunLabyInterface

  private trait FunLabyInterfaceModule extends js.Any:
    val FunLabyInterface: FunLabyInterface
  end FunLabyInterfaceModule
end UniverseFile
