package com.funlabyrinthe.editor.renderer

import scala.scalajs.js

import org.scalajs.dom

import com.raquo.laminar.api.L.{*, given}

import com.funlabyrinthe.coreinterface.*
import com.funlabyrinthe.graphics.html.Conversions.htmlKeyEvent2core

import com.funlabyrinthe.editor.renderer.LaminarUtils.*

import be.doeraene.webcomponents.ui5
import be.doeraene.webcomponents.ui5.configkeys.ToolbarAlign
import be.doeraene.webcomponents.ui5.configkeys.IconName

class ProjectRunner(val universeFile: UniverseFile, returnToProjectSelector: Observer[Unit])(using ErrorHandler):
  val runningGame = universeFile.universe.startGame()

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
      makeRunnerCanvas(),
    )
  end topElement

  def makeRunnerCanvas(): CanvasElement =
    canvasTag(
      onMountUnmountCallbackWithState(
        mount = { ctx =>
          new PlayerCanvasState(runningGame.players.head, ctx.thisNode.ref).init()
        },
        unmount = { (element, optState) =>
          for state <- optState do
            state.destroy()
        },
      ),
    )
  end makeRunnerCanvas

  final class PlayerCanvasState(player: Player, canvas: dom.HTMLCanvasElement):
    private var lastAnimationRequestHandle: Int = 0
    private var lastMillis = Double.NaN

    private val onKeyDownListener: js.Function1[dom.KeyboardEvent, Unit] = { event =>
      val coreEvent = htmlKeyEvent2core(event)

      val intfEvent = new KeyboardEvent {
        val physicalKey = coreEvent.physicalKey.toString()
        val keyString = coreEvent.keyString
        val repeat = coreEvent.repeat
        val shiftDown = coreEvent.shiftDown
        val controlDown = coreEvent.controlDown
        val altDown = coreEvent.altDown
        val metaDown = coreEvent.metaDown
      }

      player.keyDown(intfEvent)
    }

    private def scheduleRedraw(): Unit =
      lastAnimationRequestHandle = dom.window.requestAnimationFrame { currentMillis =>
        val currentMillis1 = Math.floor(currentMillis)
        if !lastMillis.isNaN then
          runningGame.advanceTickCount(currentMillis1 - lastMillis)
        lastMillis = currentMillis1

        canvas.width = player.viewWidth.toInt
        canvas.height = player.viewHeight.toInt
        player.drawView(canvas)

        scheduleRedraw()
      }
    end scheduleRedraw

    def init(): this.type =
      scheduleRedraw()
      dom.document.addEventListener("keydown", onKeyDownListener)
      this
    end init

    def destroy(): Unit =
      if lastAnimationRequestHandle != 0 then
        dom.window.cancelAnimationFrame(lastAnimationRequestHandle)
      dom.document.removeEventListener("keydown", onKeyDownListener)
    end destroy
  end PlayerCanvasState
end ProjectRunner
