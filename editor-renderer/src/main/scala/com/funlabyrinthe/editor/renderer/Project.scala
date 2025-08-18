package com.funlabyrinthe.editor.renderer

import scala.collection.mutable

import scala.scalajs.js
import scala.scalajs.js.JSConverters.*

import java.io.IOException

import com.funlabyrinthe.coreinterface.{FunLabyInterface, GlobalConfig, Universe}

import com.funlabyrinthe.editor.common.FileService.ProjectLoadInfo
import com.funlabyrinthe.editor.common.model.*

import com.funlabyrinthe.editor.renderer.electron.fileService

final class Project(
  initProjectDef: ProjectDef,
  loadInfo: ProjectLoadInfo,
  isEditing: Boolean,
):
  import Project.*

  private var _universe: Option[Universe] = None

  val projectID = initProjectDef.id
  val isLibrary = initProjectDef.projectFileContent.isLibrary

  var dependencies: List[Dependency] = initProjectDef.projectFileContent.dependencies
  val sourceFiles: mutable.ArrayBuffer[String] =
    mutable.ArrayBuffer(loadInfo.sourceFiles.toList*)

  var moduleClassNames: List[String] = Nil

  var onResourceLoaded: () => Unit = () => ()

  unpickle(initProjectDef.projectFileContent)

  def loadUniverse(): (Universe, List[PicklingError]) =
    val universeFileContent = loadInfo.universeFileContent.getOrElse {
      throw IOException("An error occured while loading the universe file")
    }
    val intf = loadFunLabyInterface(loadInfo.runtimeURI)
    val result = JSPI.await(intf.loadUniverse(moduleClassNames.toJSArray, universeFileContent, makeGlobalConfig()))
    (result._1, result._2.toList.map(PicklingError.fromInterface(_)))
  end loadUniverse

  def installUniverse(universe: Universe): Unit =
    assert(_universe.isEmpty)
    _universe = Some(universe)

  def universe: Option[Universe] = _universe

  private def makeGlobalConfig(): GlobalConfig = new {
    this.isEditing = Project.this.isEditing
    this.onResourceLoaded = () => Project.this.onResourceLoaded()
  }

  private def unpickle(projectFileContent: ProjectFileContent): Unit =
    moduleClassNames = projectFileContent.modules
  end unpickle

  def save(): Unit =
    val pickle = this.pickle()
    val pickleString = ProjectFileContent.stringifyProject(pickle) + "\n"

    JSPI.await(fileService.saveProject(projectID.id, pickleString))
  end save

  private def pickle(): ProjectFileContent =
    ProjectFileContent(
      isLibrary = isLibrary,
      dependencies = dependencies,
      modules = moduleClassNames,
    )
  end pickle
end Project

object Project:
  private def loadFunLabyInterface(runtimeURI: String): FunLabyInterface =
    JSPI.await(js.`import`[FunLabyInterfaceModule](runtimeURI)).FunLabyInterface
  end loadFunLabyInterface

  private trait FunLabyInterfaceModule extends js.Any:
    val FunLabyInterface: FunLabyInterface
  end FunLabyInterfaceModule
end Project
