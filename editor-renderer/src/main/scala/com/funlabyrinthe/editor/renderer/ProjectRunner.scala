package com.funlabyrinthe.editor.renderer

import scala.scalajs.js

import org.scalajs.dom

import com.raquo.laminar.api.L.{*, given}

import com.funlabyrinthe.coreinterface.*

import com.funlabyrinthe.editor.renderer.LaminarUtils.*

import be.doeraene.webcomponents.ui5
import be.doeraene.webcomponents.ui5.configkeys.ToolbarAlign
import be.doeraene.webcomponents.ui5.configkeys.IconName

class ProjectRunner(val project: Project, returnToProjectSelector: Observer[Unit])(using ErrorHandler):
  import ProjectRunner.*

  val runningGame = project.universe.getOrElse {
    throw IllegalArgumentException("Cannot start a game without a universe")
  }.startGame()

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
      val normalizedPhysicalKey = physicalKeyNormalizations.getOrElse(event.code, event.code)

      val intfEvent = new KeyboardEvent {
        val physicalKey = normalizedPhysicalKey
        val keyString = event.key
        val repeat = event.repeat
        val shiftDown = event.shiftKey
        val controlDown = event.ctrlKey
        val altDown = event.altKey
        val metaDown = event.metaKey
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

object ProjectRunner:
  private val physicalKeyNormalizations: Map[String, String] =
    Map(
      "VolumeDown" -> "AudioVolumeDown",
      "VolumeMute" -> "AudioVolumeMute",
      "VolumeUp" -> "AudioVolumeUp",
    )
  end physicalKeyNormalizations
end ProjectRunner
