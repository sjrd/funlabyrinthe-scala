package com.funlabyrinthe.editor.renderer

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import scala.scalajs.js

import com.raquo.laminar.api.L.{*, given}

import be.doeraene.webcomponents.ui5
import be.doeraene.webcomponents.ui5.configkeys.{ButtonDesign, IconName, ValueState}


import com.funlabyrinthe.coreinterface.*

import com.funlabyrinthe.editor.renderer.electron.fileService

class ProjectSelector(selectProjectWriter: Observer[Option[UniverseFile]])(using ErrorHandler):
  private val globalResourcesDir = File("./Resources")

  private val availableProjects = Var[List[ProjectDef]](Nil)

  locally {
    for projects <- fileService.listAvailableProjects().toFuture do
      availableProjects.set(projects.toList.map(proj =>
        ProjectDef(File(proj))
      ))
  }

  lazy val topElement: Element =
    div(
      ui5.Button(
        "New project",
        _.events.onClick --> (event => ErrorHandler.handleErrors(createNewProject())),
      ),
      ui5.Button(
        "Load project",
        _.events.onClick --> (event => ErrorHandler.handleErrors(loadProject())),
      ),
      ui5.Table(
        _.slots.columns := ui5.Table.column(
          "File name"
        ),
        newProjectRow(),
        children <-- availableProjects.signal.map(_.map(projectDefRow)),
      )
    )
  end topElement

  private def newProjectDialog(openDialogEvents: EventStream[Unit]): Element =
    val existingDirNames = availableProjects.signal.map(_.map(_.projectName).toSet)
    val dirName = Var[String]("")
    val closeEventBus = new EventBus[Unit]

    ui5.Dialog(
      _.showFromEvents(openDialogEvents),
      _.closeFromEvents(closeEventBus.events),
      _.headerText := "New source file",
      sectionTag(
        div(
          ui5.Label(_.forId := "sourcename", _.required := true, "Source name:"),
          ui5.Input(
            _.id := "sourcename",
            _.valueState <-- dirName.signal.combineWith(existingDirNames).map { (name, existing) =>
              if existing.contains(name) then ValueState.Error
              else ValueState.Success
            },
            _.value <-- dirName.signal,
            _.events.onChange.mapToValue --> dirName.writer,
          ),
        ),
      ),
      _.slots.footer := div(
        div(flex := "1"),
        ui5.Button(
          _.design := ButtonDesign.Emphasized,
          "Create new project",
          _.events.onClick.compose(_.sample(dirName.signal)) --> { name =>
            ErrorHandler.handleErrors {
              createNewProject(name)
                .map { universeFile =>
                  closeEventBus.emit(())
                  selectProjectWriter.onNext(Some(universeFile))
                }
            }
          },
        ),
        ui5.Button(
          _.design := ButtonDesign.Negative,
          "Cancel",
          _.events.onClick.mapToUnit --> closeEventBus.writer,
        ),
      )
    )
  end newProjectDialog

  private def newProjectRow(): HtmlElement =
    val openDialogBus = new EventBus[Unit]
    ui5.Table.row(
      _.cell(
        newProjectDialog(openDialogBus.events),
        ui5.Button(
          "New project ...",
          _.icon := IconName.create,
          _.events.onClick.mapToUnit --> openDialogBus,
        ),
      ),
    )
  end newProjectRow

  private def projectDefRow(projectDef: ProjectDef): HtmlElement =
    ui5.Table.row(
      _.cell(
        ui5.Button(
          projectDef.projectName,
          _.events.onClick --> { (event) =>
            ErrorHandler.handleErrors {
              loadOneProject(projectDef)
            }
          },
        ),
      ),
    )
  end projectDefRow

  private def createNewProject(): Future[Unit] =
    for
      projectFile <- selectNewProjectFile()
      universeFile <- UniverseFile.createNew(projectFile, globalResourcesDir)
    yield
      selectProjectWriter.onNext(Some(universeFile))
  end createNewProject

  private def createNewProject(projectName: String): Future[UniverseFile] =
    for
      projectDir <- fileService.createNewProject(projectName).toFuture.map(File(_))
      projectFile = projectDir / "project.funlaby"
      universeFile <- UniverseFile.createNew(projectFile, globalResourcesDir)
      _ <- universeFile.save()
    yield
      universeFile
  end createNewProject

  private def loadOneProject(projectDef: ProjectDef): Future[Unit] =
    for
      universeFile <- UniverseFile.load(projectDef.projectDir / "project.funlaby", globalResourcesDir)
    yield
      selectProjectWriter.onNext(Some(universeFile))
  end loadOneProject

  private def loadProject(): Future[Unit] =
    for
      projectFile <- selectExistingProjectFile()
      universeFile <- UniverseFile.load(projectFile, globalResourcesDir)
    yield
      selectProjectWriter.onNext(Some(universeFile))
  end loadProject

  private def selectNewProjectFile(): Future[File] =
    for result <- fileService.showSaveNewProjectDialog().toFuture yield
      result.map(new File(_)).getOrElse(UserCancelException.cancel())
  end selectNewProjectFile

  private def selectExistingProjectFile(): Future[File] =
    for result <- fileService.showOpenProjectDialog().toFuture yield
      result.map(new File(_)).getOrElse(UserCancelException.cancel())
  end selectExistingProjectFile
end ProjectSelector
