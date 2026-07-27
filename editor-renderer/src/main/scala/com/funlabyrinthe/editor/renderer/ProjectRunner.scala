package com.funlabyrinthe.editor.renderer

import scala.util.{Failure, Success, Try}

import scala.collection.mutable
import scala.scalajs.js
import scala.scalajs.js.annotation.*
import scala.scalajs.js.typedarray.*

import org.scalajs.dom

import com.raquo.laminar.api.L.{*, given}

import com.funlabyrinthe.coreinterface.*
import com.funlabyrinthe.coreinterface as intf

import com.funlabyrinthe.editor.renderer.LaminarUtils.*

import be.doeraene.webcomponents.ui5
import be.doeraene.webcomponents.ui5.configkeys.{BusyIndicatorSize, IconName, MessageStripDesign, ToolbarAlign}

class ProjectRunner(val project: Project, returnToProjectSelector: Observer[Unit])(using ErrorHandler):
  import ProjectRunner.*

  val runningGame: Signal[Option[RunningGame]] =
    Signal.fromJsPromise(JSPI.async {
      val (universe, errors) = project.loadUniverse()
      if errors.nonEmpty then
        throw IllegalStateException(
          "There were errors while loading the game:"
          + errors.mkString("\n", "\n", "")
        )
      else
        universe.startGame()
    })
  end runningGame

  val topElement: Element =
    div(
      ui5.Toolbar(
        _.alignContent := ToolbarAlign.Start,
        _.button(
          _.icon(IconName.`sys-back`),
          _.text("Back to project selector"),
          _.events.onClick.mapToUnit --> returnToProjectSelector,
        ),
      ),
      child <-- runningGame.recoverToTry.map {
        case Success(None) =>
          ui5.BusyIndicator(
            _.size := BusyIndicatorSize.L,
            _.active := true,
          )
        case Success(Some(game)) =>
          indigoGameElement(game)
        case Failure(exception) =>
          ui5.MessageStrip(
            _.design := MessageStripDesign.Negative,
            ErrorHandler.exceptionToString(exception),
            _.hideCloseButton := true,
          )
      }
    )
  end topElement

  def indigoGameElement(game: RunningGame): Div = {
    div(
      idAttr := "indigo-container",
      onMountUnmountCallbackWithState[Div, GameRunner]({ ctx =>
        GameRunner.start(ctx.thisNode.ref, game)
      }, { (thisNode, optIndigoUI) =>
        for indigoUI <- optIndigoUI do
          indigoUI.halt()
      }),
    )
  }
end ProjectRunner

object ProjectRunner:
  object GameRunner {
    @JSImport("../../../../game-runner/target/scala-3.8.3/funlaby-game-runner-fastopt/main.js", "start")
    @js.native
    def start(container: dom.HTMLElement, runningGame: RunningGame): GameRunner = js.native
  }

  trait GameRunner extends js.Object {
    def halt(): Unit
  }
end ProjectRunner
