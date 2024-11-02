package com.funlabyrinthe.editor.renderer

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import scala.scalajs.js
import scala.scalajs.js.annotation.*

import org.scalajs.dom
import org.scalajs.dom.HTMLElement

import com.raquo.laminar.api.L.{*, given}

import be.doeraene.webcomponents.ui5
import be.doeraene.webcomponents.ui5.configkeys.ButtonDesign

object Renderer:
  def main(args: Array[String]): Unit =
    // https://github.com/raquo/Laminar/issues/175
    if dom.document.readyState == "loading" then
      renderOnDomContentLoaded(dom.document.body, new Renderer().appElement)
    else
      render(dom.document.body, new Renderer().appElement)
  end main

  enum TopLevelState:
    case NoProject
    case Editing(project: Project)
    case Playing(project: Project)
end Renderer

class Renderer:
  import Renderer.*

  val errorHandlingBus = new EventBus[Option[Throwable]]
  given ErrorHandler =
    new ErrorHandler(errorHandlingBus.writer.contramap(Some(_).filter(!_.isInstanceOf[UserCancelException])))

  val currentError: Signal[Option[Throwable]] = errorHandlingBus.events.toSignal(None, false)

  val askConfirmationVar = Var[Option[(String, () => Future[Unit])]](None)

  given Dialogs =
    new Dialogs(
      askConfirmationWriter = askConfirmationVar.writer.contramap(Some(_))
    )
  end given

  val projectVar: Var[TopLevelState] = Var(TopLevelState.NoProject)
  val projectSignal = projectVar.signal.distinct

  val returnToProjectSelector: Observer[Unit] = projectVar.writer.contramap(_ => TopLevelState.NoProject)

  val appElement: Element =
    div(
      cls := "fill-parent-height",
      errorHandlingDialog,
      askConfirmationDialog,
      child <-- projectSignal.map { project =>
        project match
          case TopLevelState.NoProject        => new ProjectSelector(projectVar.writer).topElement
          case TopLevelState.Editing(project) => new UniverseEditor(project, returnToProjectSelector).topElement
          case TopLevelState.Playing(project) => new ProjectRunner(project, returnToProjectSelector).topElement
      }
    )
  end appElement

  lazy val errorHandlingDialog: Element =
    val actualError = currentError.map(_.map(findActualError(_)))
    ui5.Dialog(
      _.showFromEvents(errorHandlingBus.events.filter(_.isDefined).mapTo(())),
      _.closeFromEvents(errorHandlingBus.events.filter(_.isEmpty).mapTo(())),
      _.headerText := "Error",
      _.state := ui5.configkeys.ValueState.Negative,
      sectionTag(
        child <-- actualError.map(_.fold("")(_.getMessage())),
      ),
      _.slots.footer := div(
        div(flex := "1"),
        ui5.Button(
          _.design := ui5.configkeys.ButtonDesign.Emphasized,
          "Dismiss",
          _.events.onClick.mapTo(None) --> errorHandlingBus.writer,
        ),
      ),
    )
  end errorHandlingDialog

  private def findActualError(error: Throwable): Throwable = error match
    case error: java.util.concurrent.ExecutionException =>
      if error.getCause() == null then error
      else error.getCause()
    case _ =>
      error
  end findActualError

  private lazy val askConfirmationDialog: Element =
    val askConfirmationSignal = askConfirmationVar.signal
    val askConfirmationChanges = askConfirmationSignal.changes
    ui5.Dialog(
      _.showFromEvents(askConfirmationChanges.filter(_.isDefined).map(_ => ())),
      _.closeFromEvents(askConfirmationChanges.filter(_.isEmpty).map(_ => ())),
      _.draggable := true,
      _.headerText := "Confirmation",
      sectionTag(
        child <-- askConfirmationSignal.map(_.fold("")(_._1)),
      ),
      _.slots.footer := div(
        div(flex := "1"),
        ui5.Button(
          _.design := ButtonDesign.Emphasized,
          "OK",
          _.events.onClick.compose(_.sample(askConfirmationSignal)) --> { askConfirmation =>
            askConfirmationVar.set(None)
            for askConfirm <- askConfirmation do
              ErrorHandler.handleErrors {
                askConfirm._2()
              }
          },
        ),
        ui5.Button(
          _.design := ButtonDesign.Negative,
          "Cancel",
          _.events.onClick.mapTo(None) --> askConfirmationVar.writer,
        ),
      )
    )
  end askConfirmationDialog
end Renderer
