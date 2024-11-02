package com.funlabyrinthe.editor.renderer

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import scala.scalajs.js

import com.raquo.laminar.api.L.{*, given}

import be.doeraene.webcomponents.ui5
import be.doeraene.webcomponents.ui5.configkeys.{ButtonDesign, IconName, ValueState}

import com.funlabyrinthe.coreinterface.*

import com.funlabyrinthe.editor.common.FileService
import com.funlabyrinthe.editor.renderer.electron.fileService
import com.funlabyrinthe.editor.renderer.model.*

class ProjectSelector(selectProjectWriter: Observer[Renderer.TopLevelState])(using ErrorHandler):
  private val availableProjects = Var[List[ProjectDef]](Nil)

  private def fileServiceProjectDefToModel(proj: FileService.ProjectDef): ProjectDef =
    ProjectDef(
      ProjectID(proj.id),
      ProjectFileContent.parseProject(proj.projectFileContent)
    )
  end fileServiceProjectDefToModel

  locally {
    for projects <- fileService.listAvailableProjects().toFuture do
      availableProjects.set(projects.toList.map(fileServiceProjectDefToModel(_)))
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
    val existingIDs = availableProjects.signal.map(_.map(_.id.id).toSet)
    val dirName = Var[String]("")
    val closeEventBus = new EventBus[Unit]

    ui5.Dialog(
      _.showFromEvents(openDialogEvents),
      _.closeFromEvents(closeEventBus.events),
      _.headerText := "New project",
      sectionTag(
        div(
          ui5.Label(_.forId := "projectid", _.required := true, "Project ID:"),
          ui5.Input(
            _.id := "projectid",
            _.valueState <-- dirName.signal.combineWith(existingIDs).map { (projectID, existing) =>
              if !ProjectID.isValidProjectID(projectID) || existing.contains(projectID) then ValueState.Negative
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
          _.events.onClick.compose(_.sample(dirName.signal)) --> { projectID =>
            ErrorHandler.handleErrors {
              createNewProject(projectID)
                .map { project =>
                  closeEventBus.emit(())
                  selectProjectWriter.onNext(Renderer.TopLevelState.Editing(project))
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

  private def createNewProject(projectID: String): Future[Project] =
    for
      js.Tuple2(projectDef, loadInfo) <- fileService.createNewProject(projectID).toFuture
      modelProjectDef = fileServiceProjectDefToModel(projectDef)
      project <- Project.createNew(modelProjectDef, loadInfo)
      _ <- project.save()
    yield
      project
  end createNewProject

  private def loadOneProject(
    projectDef: ProjectDef,
    isEditing: Boolean,
    makeState: Project => Renderer.TopLevelState,
  ): Future[Unit] =
    for
      loadInfo <- fileService.loadProject(projectDef.id.id).toFuture
      project <- Project.load(projectDef, loadInfo, isEditing)
    yield
      selectProjectWriter.onNext(makeState(project))
  end loadOneProject
end ProjectSelector
