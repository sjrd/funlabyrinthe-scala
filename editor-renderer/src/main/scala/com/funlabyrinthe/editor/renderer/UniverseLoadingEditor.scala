package com.funlabyrinthe.editor.renderer

import scala.collection.mutable
import scala.util.{Failure, Success}

import scala.scalajs.js

import org.scalajs.dom

import com.funlabyrinthe.coreinterface.Universe

import com.raquo.laminar.api.L.{*, given}

import be.doeraene.webcomponents.ui5
import be.doeraene.webcomponents.ui5.configkeys.*

import com.funlabyrinthe.editor.common.model.*

import com.funlabyrinthe.editor.renderer.LaminarUtils.*
import com.funlabyrinthe.editor.renderer.electron.{fileService, Services}

final class UniverseLoadingEditor(
  project: Project,
)(using ErrorHandler, Dialogs)
    extends Editor(project):

  import UniverseLoadingEditor.*

  val tabTitle = "Maps"

  private val universeLoadingState: Var[UniverseLoadingState] = Var({
    if project.isLibrary then
      UniverseLoadingState.NoUniverse
    else
      UniverseLoadingState.Loading
  })

  private val universeEditor: Signal[Option[UniverseEditor]] =
    universeLoadingState.signal.map {
      case UniverseLoadingState.Loaded(_, _, editor) => Some(editor)
      case _                                         => None
    }

  private def switchToUniverseEditor(universe: Universe, errors: List[PicklingError]): Unit =
    project.installUniverse(universe)
    val editor = UniverseEditor(project, universe)
    project.onResourceLoaded = { () =>
      editor.refreshUI()
    }
    universeLoadingState.set(UniverseLoadingState.Loaded(universe, withErrors = errors.nonEmpty, editor))
  end switchToUniverseEditor

  locally {
    if !project.isLibrary then
      try
        val (universe, errors) = project.loadUniverse()
        if errors.isEmpty then
          switchToUniverseEditor(universe, errors)
        else
          universeLoadingState.set(UniverseLoadingState.ErrorsToConfirm(universe, errors))
      catch case exception: Throwable =>
        universeLoadingState.set(UniverseLoadingState.FatalErrors(List(ErrorHandler.exceptionToString(exception))))
  }

  val isModified: Signal[Boolean] =
    universeEditor.flatMapSwitch {
      case None         => Signal.fromValue(false)
      case Some(editor) => editor.isModified
    }

  def saveContent(): Unit =
    universeLoadingState.now() match
      case UniverseLoadingState.Loaded(_, _, editor) =>
        editor.saveContent()
      case _ =>
        ()

  lazy val topElement: Signal[Element] =
    universeLoadingState.signal.combineWith(universeEditor).flatMapSwitch { (state, universeEditor) =>
      state match
        case UniverseLoadingState.NoUniverse =>
          Signal.fromValue(
            ui5.Text("No maps in a library project")
          )
        case UniverseLoadingState.Loading =>
          Signal.fromValue(
            ui5.BusyIndicator(
              _.size := BusyIndicatorSize.L,
              _.active := true,
            )
          )
        case UniverseLoadingState.Loaded(universe, withErrors, editor) =>
          editor.topElement
        case UniverseLoadingState.ErrorsToConfirm(universe, errors) =>
          Signal.fromValue(
            div(
              ui5.IllustratedMessage(
                _.name := IllustratedMessageType.UnableToLoad,
                _.design := IllustratedMessageSize.Dialog,
                _.titleText := "There were errors while loading the universe",
                _.subtitleText := {
                  "Would you like to open the universe anyway? "
                    + "If yes, the errors below will be ignored, leading to possible data loss."
                },
                ui5.Button(
                  "Ignore errors and open anyway",
                  _.events.onClick --> { _ =>
                    switchToUniverseEditor(universe, errors)
                  }
                ),
              ),
              ui5.NotificationList(
                errors.map { error =>
                  val fullPath = error.component.fold(error.path)(_ :: error.path)
                  ui5.NotificationList.item(
                    _.titleText := error.message,
                    fullPath.map(segment => ui5.NotificationListItem.slots.footnotes := span(segment)),
                    _.state := ValueState.Negative,
                  )
                },
              ),
            )
          )
        case UniverseLoadingState.FatalErrors(errors) =>
          Signal.fromValue(
            ui5.NotificationList(
              errors.map { error =>
                val errorLines = error.linesIterator.toList
                ui5.NotificationList.item(
                  _.titleText := errorLines.headOption.getOrElse("Unknown error"),
                  errorLines.drop(1),
                  _.state := ValueState.Negative,
                )
              },
            )
          )
    }
end UniverseLoadingEditor

object UniverseLoadingEditor:
  enum UniverseLoadingState:
    case NoUniverse
    case Loading
    case Loaded(universe: Universe, withErrors: Boolean, editor: UniverseEditor)
    case ErrorsToConfirm(universe: Universe, errors: List[PicklingError])
    case FatalErrors(errors: List[String])
end UniverseLoadingEditor
