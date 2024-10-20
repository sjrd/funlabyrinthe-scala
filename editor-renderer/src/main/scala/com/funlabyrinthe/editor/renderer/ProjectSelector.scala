package com.funlabyrinthe.editor.renderer

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import scala.scalajs.js

import com.raquo.laminar.api.L.{*, given}

import be.doeraene.webcomponents.ui5
import be.doeraene.webcomponents.ui5.configkeys.{ButtonDesign, IconName, ValueState}


import com.funlabyrinthe.coreinterface.*

import com.funlabyrinthe.editor.renderer.electron.fileService

class ProjectSelector(selectProjectWriter: Observer[Renderer.TopLevelState])(using ErrorHandler):
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
      ui5.compat.Table(
        className := "project-selector-table",
        _.slots.columns := ui5.compat.Table.column(
          ""
        ),
        _.slots.columns := ui5.compat.Table.column(
          ""
        ),
        _.slots.columns := ui5.compat.Table.column(
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
              if existing.contains(name) then ValueState.Negative
              else ValueState.Positive
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
                  selectProjectWriter.onNext(Renderer.TopLevelState.Editing(universeFile))
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
    ui5.compat.Table.row(
      _.cell(),
      _.cell(),
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
    ui5.compat.Table.row(
      _.cell(
        ui5.Button(
          _.icon := IconName.edit,
          _.events.onClick --> { (event) =>
            ErrorHandler.handleErrors {
              loadOneProject(projectDef, isEditing = true, Renderer.TopLevelState.Editing(_))
            }
          },
        ),
      ),
      _.cell(
        ui5.Button(
          _.icon := IconName.play,
          _.events.onClick --> { (event) =>
            ErrorHandler.handleErrors {
              loadOneProject(projectDef, isEditing = false, Renderer.TopLevelState.Playing(_))
            }
          },
        ),
      ),
      _.cell(
        projectDef.projectName,
      ),
    )
  end projectDefRow

  private def createNewProject(projectName: String): Future[UniverseFile] =
    for
      projectDir <- fileService.createNewProject(projectName).toFuture.map(File(_))
      projectFile = projectDir / "project.json"
      universeFile <- UniverseFile.createNew(projectFile, globalResourcesDir)
      _ <- universeFile.save()
    yield
      universeFile
  end createNewProject

  private def loadOneProject(
    projectDef: ProjectDef,
    isEditing: Boolean,
    makeState: UniverseFile => Renderer.TopLevelState,
  ): Future[Unit] =
    for
      universeFile <- UniverseFile.load(projectDef.projectDir / "project.json", globalResourcesDir, isEditing)
    yield
      selectProjectWriter.onNext(makeState(universeFile))
  end loadOneProject
end ProjectSelector
