package com.funlabyrinthe.editor.renderer

import scala.collection.mutable
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import scala.scalajs.js
import scala.scalajs.js.JSConverters.*

import java.io.IOException

import com.funlabyrinthe.coreinterface.{FunLabyInterface, GlobalEventHandler, Universe}
import com.funlabyrinthe.editor.renderer.electron.fileService
import com.funlabyrinthe.editor.renderer.model.ProjectDef

final class UniverseFile private (
  initProjectDef: ProjectDef,
  coreLibs: js.Array[String],
  val intf: FunLabyInterface,
  isEditing: Boolean,
):
  import UniverseFile.*

  private var _universe: Option[Universe] = None

  val projectID = initProjectDef.id
  val baseURI = File(initProjectDef.baseURI)

  val universeFile: File = baseURI / "universe.json"
  val sourcesDirectory: File = baseURI / "sources"
  val targetDirectory: File = baseURI / "target"

  val dependencyClasspath = coreLibs.filter(!ScalaLibraryName.matches(_)).map(new File(_))
  val fullClasspath = coreLibs.map(new File(_)) :+ targetDirectory

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
    val f = for
      universePickleString <- universeFile.readAsString()
    yield
      unpickle(initProjectDef.projectFileContent)

      for universe <- intf.loadUniverse(moduleClassNames.toJSArray, universePickleString, makeGlobalEventHandler()).toFuture yield
        _universe = Some(universe)
    end f

    f.flatten.map(_ => this)
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

    (baseURI / "project.json").writeString(pickleString)
      .flatMap(_ => universeFile.writeString(universePickleString))
  end save

  private def pickle(): ProjectFileContent =
    ProjectFileContent(
      modules = moduleClassNames,
      sources = sourceFiles.toList,
    )
  end pickle
end UniverseFile

object UniverseFile:
  private val ScalaLibraryName = raw"""/(?:scala-library|scala3-library_3)-[.0-9]+\.jar$$""".r
  private val coreBridgeModulePath =
    "./../../../../core-bridge/target/scala-3.5.1/funlaby-core-bridge-fastopt/main.js"

  def createNew(projectDef: ProjectDef, globalResourcesDir: File): Future[UniverseFile] =
    for
      coreLibs <- fileService.funlabyCoreLibs().toFuture
      intf <- loadFunLabyInterface(projectDef.baseURI)
      universeFile <- new UniverseFile(projectDef, coreLibs, intf, isEditing = false).createNew()
    yield
      universeFile
  end createNew

  def load(projectDef: ProjectDef, globalResourcesDir: File, isEditing: Boolean): Future[UniverseFile] =
    for
      coreLibs <- fileService.funlabyCoreLibs().toFuture
      intf <- loadFunLabyInterface(projectDef.baseURI)
      universeFile <- new UniverseFile(projectDef, coreLibs, intf, isEditing).load()
    yield
      universeFile
  end load

  private def loadFunLabyInterface(projectBaseURI: String): Future[FunLabyInterface] =
    val runtimeUnderTestURI = s"$projectBaseURI/runtime-under-test/main.js"

    def load(modulePath: String): Future[FunLabyInterfaceModule] =
      js.`import`[FunLabyInterfaceModule](modulePath).toFuture

    val loadedModule =
      load(runtimeUnderTestURI).recoverWith {
        case js.JavaScriptException(e) =>
          println(s"could not load $runtimeUnderTestURI; falling back on default")
          load(coreBridgeModulePath)
      }

    loadedModule.map(_.FunLabyInterface)
  end loadFunLabyInterface

  private trait FunLabyInterfaceModule extends js.Any:
    val FunLabyInterface: FunLabyInterface
  end FunLabyInterfaceModule
end UniverseFile
