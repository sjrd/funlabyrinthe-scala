package com.funlabyrinthe.editor.renderer

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}
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
      project.loadUniverse().onComplete {
        case Success((universe, Nil)) =>
          project.installUniverse(universe)
          val editor = UniverseEditor(project, universe)
          project.onResourceLoaded = { () =>
            editor.refreshUI()
          }
          universeLoadingState.set(UniverseLoadingState.Loaded(universe, withErrors = false, editor))
        case Success((universe, errors)) =>
          universeLoadingState.set(UniverseLoadingState.ErrorsToConfirm(universe, errors))
        case Failure(exception) =>
          universeLoadingState.set(UniverseLoadingState.FatalErrors(List(ErrorHandler.exceptionToString(exception))))
      }
  }

  val isModified: Signal[Boolean] =
    universeEditor.flatMapSwitch {
      case None         => Signal.fromValue(false)
      case Some(editor) => editor.isModified
    }

  def saveContent()(using ExecutionContext): Future[Unit] =
    universeLoadingState.now() match
      case UniverseLoadingState.Loaded(_, _, editor) =>
        editor.saveContent()
      case _ =>
        Future.successful(())

  lazy val topElement: Element =
    div(
      child <-- universeLoadingState.signal.combineWith(universeEditor).map { (state, universeEditor) =>
        state match
          case UniverseLoadingState.NoUniverse =>
            ui5.Text("No maps in a library project")
          case UniverseLoadingState.Loading =>
            ui5.BusyIndicator(
              _.size := BusyIndicatorSize.L,
              _.active := true,
            )
          case UniverseLoadingState.Loaded(universe, withErrors, editor) =>
            editor.topElement
          case UniverseLoadingState.ErrorsToConfirm(universe, errors) =>
            ???
          case UniverseLoadingState.FatalErrors(errors) =>
            ui5.NotificationList(
              errors.map { error =>
                ui5.NotificationList.item(
                  _.titleText := error,
                  _.state := ValueState.Negative,
                )
              },
            )
      },
    )
end UniverseLoadingEditor

object UniverseLoadingEditor:
  enum UniverseLoadingState:
    case NoUniverse
    case Loading
    case Loaded(universe: Universe, withErrors: Boolean, editor: UniverseEditor)
    case ErrorsToConfirm(universe: Universe, errors: List[String])
    case FatalErrors(errors: List[String])
end UniverseLoadingEditor
