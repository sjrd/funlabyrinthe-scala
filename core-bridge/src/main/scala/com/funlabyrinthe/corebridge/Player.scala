package com.funlabyrinthe.corebridge

import scala.scalajs.js

import org.scalajs.dom

import com.funlabyrinthe.core
import com.funlabyrinthe.core.Control
import com.funlabyrinthe.core.input.KeyEvent
import com.funlabyrinthe.core.graphics.{DrawContext, Rectangle2D}
import com.funlabyrinthe.graphics.html.CanvasWrapper

import com.funlabyrinthe.coreinterface as intf

final class Player(underlying: core.CorePlayer) extends intf.Player:
  import intf.Player.*

  def controller: core.Controller = underlying.controller

  def viewWidth: Double = controller.viewSize._1
  def viewHeight: Double = controller.viewSize._2

  def drawView(canvas: dom.HTMLCanvasElement): Unit =
    val rect = Rectangle2D(0, 0, canvas.width, canvas.height)
    val gc = new CanvasWrapper(canvas).getGraphicsContext2D()
    val ctx = new DrawContext(gc, rect)
    controller.drawView(ctx)
  end drawView

  private var playerBusy: Boolean = false
  private var keyEventCont: Option[KeyEvent => Control[Any]] = None

  private def processControlResult(controlResult: Control[Any]): Unit = {
    controlResult match {
      case Control.Done(_) =>
        playerBusy = false

      case Control.Sleep(ms, cont) =>
        js.timers.setTimeout(ms) {
          processControlResult(cont(()))
        }

      case Control.WaitForKeyEvent(cont) =>
        keyEventCont = Some(cont)
    }
  }

  def keyDown(code: KeyCode, shiftDown: Boolean, controlDown: Boolean,
      altDown: Boolean, metaDown: Boolean): Unit =
    val coreCode = code match
      case KeyCode.Enter => core.input.KeyCode.Enter
      case KeyCode.Left  => core.input.KeyCode.Left
      case KeyCode.Up    => core.input.KeyCode.Up
      case KeyCode.Right => core.input.KeyCode.Right
      case KeyCode.Down  => core.input.KeyCode.Down
      case KeyCode.Other => core.input.KeyCode.Other

    val keyEvent = KeyEvent(coreCode, shiftDown, controlDown, altDown, metaDown)

    if keyEventCont.isDefined then
      val cont = keyEventCont.get
      keyEventCont = None
      processControlResult(cont(keyEvent))
    else if !playerBusy then
      playerBusy = true
      processControlResult(controller.onKeyEvent(keyEvent))
    end if
  end keyDown
end Player
