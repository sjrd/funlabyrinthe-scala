package com.funlabyrinthe.editor.renderer

import scala.collection.mutable
import scala.util.{Failure, Success}

import scala.scalajs.js

import org.scalajs.dom

import com.funlabyrinthe.coreinterface.*

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

  locally {
    if !project.isLibrary then
      try
        val (universe, errors) = project.loadUniverse()
        if errors.isEmpty then
          project.installUniverse(universe)
          val editor = UniverseEditor(project, universe)
          project.onResourceLoaded = { () =>
            editor.refreshUI()
          }
          universeLoadingState.set(UniverseLoadingState.Loaded(universe, withErrors = false, editor))
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
          ???
        case UniverseLoadingState.FatalErrors(errors) =>
          Signal.fromValue(
            ui5.NotificationList(
              errors.map { error =>
                ui5.NotificationList.item(
                  _.titleText := error,
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
    case ErrorsToConfirm(universe: Universe, errors: List[String])
    case FatalErrors(errors: List[String])
end UniverseLoadingEditor
