package com.funlabyrinthe.editor.renderer

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

import scala.scalajs.js

import org.scalajs.dom

import com.raquo.laminar.api.L.{*, given}

import be.doeraene.webcomponents.ui5
import be.doeraene.webcomponents.ui5.configkeys.{BarDesign, ButtonDesign, IconName, ListMode}

import com.funlabyrinthe.editor.common.model.*

import com.funlabyrinthe.editor.renderer.LaminarUtils.*
import com.funlabyrinthe.editor.renderer.electron.{fileService, Services}

final class ProjectSettingsEditor(
  project: Project,
)(using ErrorHandler, Dialogs)
    extends Editor(project):

  val tabTitle = "Settings"

  private val isModifiedVar: Var[Boolean] = Var(false)
  val isModified: Signal[Boolean] = isModifiedVar.signal

  private val isEditingDependenciesVar = Var[Boolean](false)
  private val dependenciesVar = Var[List[Dependency]](project.dependencies.sorted)

  private def markModified(): Unit = isModifiedVar.set(true)

  def saveContent()(using ExecutionContext): Future[Unit] =
    project.save()
      .map(_ => isModifiedVar.set(false))
  end saveContent

  lazy val topElement: Element =
    div(
      librariesPanel,
    )
  end topElement

  private lazy val librariesPanel: Element =
    def ifEditing[A](f: => A): Signal[Option[A]] =
      isEditingDependenciesVar.signal.map(editing => if editing then Some(f) else None)

    def filterApplicableLibraries(availableProjects: List[ProjectDef]): List[ProjectDef] =
      var allLibs = availableProjects
        .filter(p => p.projectFileContent.isLibrary)

      val reverseDependencyPairs = for
        dependent <- allLibs
        dependency <- dependent.projectFileContent.dependencies
      yield
        dependency.projectID -> dependent.id
      val reverseDependencies = reverseDependencyPairs.groupMap(_._1)(_._2)

      val unsafeLibs = mutable.Set.empty[ProjectID]

      def rec(unsafeProjectID: ProjectID): Unit =
        if unsafeLibs.add(unsafeProjectID) then
          for dependent <- reverseDependencies.getOrElse(unsafeProjectID, Nil) do
            rec(dependent)

      rec(project.projectID)

      allLibs.filterNot(lib => unsafeLibs.contains(lib.id))
    end filterApplicableLibraries

    ui5.Panel(
      _.headerText := "Libraries",
      ui5.DynamicSideContent(
        _.equalSplit := true,
        ui5.UList(
          _.headerText := "Current libraries",
          _.noDataText := "This project currently does not use any library",
          _.selectionMode <-- isEditingDependenciesVar.signal.map { editing =>
            if editing then ListMode.Delete
            else ListMode.None
          },
          children <-- dependenciesVar.signal.split(_.projectID) { (_, initial, sig) =>
            ui5.UList.item(
              initial.projectID.toString(),
              _.description <-- sig.map(_.version.displayString),
              _.slots.deleteButton := ui5.Button(
                _.design := ButtonDesign.Transparent,
                _.icon := IconName.delete,
                _.tooltip := "Remove",
                onClick --> { _ =>
                  dependenciesVar.update(_.filterNot(_.projectID == initial.projectID))
                },
              ),
            )
          },
        ),
        _.slots.sideContent <-- ifEditing {
          val availableLibs: Signal[List[ProjectDef]] = Signal.fromFuture(
            Services.listAvailableProjects().map { availableProjects =>
              filterApplicableLibraries(availableProjects).sortBy(_.id)
            },
            Nil
          ).combineWith(dependenciesVar.signal).map { (libs, deps) =>
            libs.filterNot(lib => deps.exists(_.projectID == lib.id))
          }
          ui5.UList(
            _.headerText := "Available libraries",
            _.selectionMode := ListMode.Delete, // abuse
            children <-- availableLibs.split(_.id) { (_, initial, sig) =>
              ui5.UList.item(
                initial.id.toString(),
                _.slots.deleteButton := ui5.Button(
                  _.design := ButtonDesign.Transparent,
                  _.icon := IconName.add,
                  onClick --> { _ =>
                    dependenciesVar.update { deps =>
                      (Dependency(initial.id, DependencyVersion.LocalCurrent) :: deps).sorted
                    }
                  },
                ),
              )
            },
          )
        }.map(_.toSeq),
      ),
      ui5.Bar(
        _.design := BarDesign.Footer,
        _.slots.startContent <-- isEditingDependenciesVar.signal.map { (isEditing) =>
          if !isEditing then
            Seq(
              ui5.Button(
                _.icon := IconName.edit,
                "Enable editing libraries",
                _.events.onClick.mapTo(true) --> isEditingDependenciesVar.writer,
              ),
            )
          else
            Seq(
              ui5.Button(
                _.icon := IconName.accept,
                _.design := ButtonDesign.Positive,
                "Confirm new libraries",
                _.events.onClick.compose(_.sample(dependenciesVar)) --> { deps =>
                  ErrorHandler.handleErrorsSync {
                    commitLibraries(deps)
                    markModified()
                    isEditingDependenciesVar.set(false)
                  }
                },
              ),
              ui5.Button(
                _.icon := IconName.cancel,
                _.design := ButtonDesign.Negative,
                "Cancel editing libraries",
                _.events.onClick --> { _ =>
                  isEditingDependenciesVar.set(false)
                  dependenciesVar.set(project.dependencies)
                },
              ),
            )
          end if
        },
      ),
    )
  end librariesPanel

  private def commitLibraries(dependencies: List[Dependency]): Unit =
    project.dependencies = dependencies
  end commitLibraries
end ProjectSettingsEditor
