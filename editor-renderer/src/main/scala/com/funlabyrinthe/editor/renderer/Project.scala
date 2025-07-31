package com.funlabyrinthe.editor.renderer

import scala.collection.mutable
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import scala.scalajs.js
import scala.scalajs.js.JSConverters.*

import java.io.IOException

import com.funlabyrinthe.coreinterface.{FunLabyInterface, GlobalConfig, Universe}

import com.funlabyrinthe.editor.common.FileService.ProjectLoadInfo
import com.funlabyrinthe.editor.common.model.*

import com.funlabyrinthe.editor.renderer.electron.fileService

final class Project private (
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

  private def installUniverse(universe: Universe): Unit =
    assert(_universe.isEmpty)
    _universe = Some(universe)

  def universe: Option[Universe] = _universe

  private def createNew(): this.type =
    val defaultModules = List("com.funlabyrinthe.mazes.Mazes")
    moduleClassNames = defaultModules
    this
  end createNew

  private def load(): this.type =
    unpickle(initProjectDef.projectFileContent)
    this
  end load

  private def makeGlobalConfig(): GlobalConfig = new {
    this.isEditing = Project.this.isEditing
    this.onResourceLoaded = () => Project.this.onResourceLoaded()
  }

  private def unpickle(projectFileContent: ProjectFileContent): Unit =
    moduleClassNames = projectFileContent.modules
  end unpickle

  def save(): Future[Unit] =
    val pickle = this.pickle()
    val pickleString = ProjectFileContent.stringifyProject(pickle)

    val universePickleString = universe.map(_.save()).orUndefined

    fileService.saveProject(projectID.id, pickleString, universePickleString).toFuture
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
  def createNew(projectDef: ProjectDef, loadInfo: ProjectLoadInfo): Future[Project] =
    val project = new Project(projectDef, loadInfo, isEditing = true).createNew()
    if project.isLibrary then
      Future.successful(project)
    else
      for
        intf <- loadFunLabyInterface(loadInfo.runtimeURI)
        universe <- intf.createNewUniverse(project.moduleClassNames.toJSArray, project.makeGlobalConfig()).toFuture
      yield
        project.installUniverse(universe)
        project
  end createNew

  def load(projectDef: ProjectDef, loadInfo: ProjectLoadInfo,
      isEditing: Boolean): Future[Project] =
    val project = new Project(projectDef, loadInfo, isEditing).load()
    loadInfo.universeFileContent.fold {
      if !isEditing && projectDef.projectFileContent.isLibrary then
        Future.failed(IllegalArgumentException("Cannot load a library for playing"))
      else if !isEditing && loadInfo.universeFileContent.isEmpty then
        Future.failed(IOException("An error happened while loading the universe file for playing"))
      else
        Future.successful(project)
    } { universeFileContent =>
      for
        intf <- loadFunLabyInterface(loadInfo.runtimeURI)
        universe <- intf.loadUniverse(project.moduleClassNames.toJSArray, universeFileContent, project.makeGlobalConfig()).toFuture
      yield
        project.installUniverse(universe)
        project
    }
  end load

  private def loadFunLabyInterface(runtimeURI: String): Future[FunLabyInterface] =
    js.`import`[FunLabyInterfaceModule](runtimeURI).toFuture.map(_.FunLabyInterface)
  end loadFunLabyInterface

  private trait FunLabyInterfaceModule extends js.Any:
    val FunLabyInterface: FunLabyInterface
  end FunLabyInterfaceModule
end Project
