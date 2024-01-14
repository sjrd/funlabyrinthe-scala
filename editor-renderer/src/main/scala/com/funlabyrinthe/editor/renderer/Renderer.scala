package com.funlabyrinthe.editor.renderer

import scala.scalajs.js
import scala.scalajs.js.annotation.*

import org.scalajs.dom
import org.scalajs.dom.HTMLElement

import com.raquo.laminar.api.L.{*, given}

import be.doeraene.webcomponents.ui5

object Renderer:
  def main(args: Array[String]): Unit =
    renderOnDomContentLoaded(dom.document.body, new Renderer().appElement)

  enum TopLevelState:
    case NoProject
    case Editing(universeFile: UniverseFile)
    case Playing(universeFile: UniverseFile)
end Renderer

class Renderer:
  import Renderer.*

  val errorHandlingBus = new EventBus[Option[Throwable]]
  given ErrorHandler =
    new ErrorHandler(errorHandlingBus.writer.contramap(Some(_).filter(!_.isInstanceOf[UserCancelException])))

  val currentError: Signal[Option[Throwable]] = errorHandlingBus.events.toSignal(None, false)

  val universeFileVar: Var[TopLevelState] = Var(TopLevelState.NoProject)
  val universeFileSignal = universeFileVar.signal.distinct

  val returnToProjectSelector: Observer[Unit] = universeFileVar.writer.contramap(_ => TopLevelState.NoProject)

  val appElement: Element =
    div(
      cls := "fill-parent-height",
      errorHandlingDialog,
      child <-- universeFileSignal.map { universeFile =>
        universeFile match
          case TopLevelState.NoProject             => new ProjectSelector(universeFileVar.writer).topElement
          case TopLevelState.Editing(universeFile) => new UniverseEditor(universeFile, returnToProjectSelector).topElement
          case TopLevelState.Playing(universeFile) => new ProjectRunner(universeFile, returnToProjectSelector).topElement
      }
    )
  end appElement

  lazy val errorHandlingDialog: Element =
    val actualError = currentError.map(_.map(findActualError(_)))
    ui5.Dialog(
      _.showFromEvents(errorHandlingBus.events.filter(_.isDefined).mapTo(())),
      _.closeFromEvents(errorHandlingBus.events.filter(_.isEmpty).mapTo(())),
      _.headerText := "Error",
      _.state := ui5.configkeys.ValueState.Error,
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
end Renderer
