package com.funlabyrinthe.corebridge

import scala.scalajs.js

import org.scalajs.dom

import com.funlabyrinthe.core
import com.funlabyrinthe.core.Control
import com.funlabyrinthe.core.input.{KeyEvent, PhysicalKey}
import com.funlabyrinthe.core.graphics.{DrawContext, Rectangle2D}
import com.funlabyrinthe.graphics.html.CanvasWrapper

import com.funlabyrinthe.coreinterface as intf

final class Player(underlying: core.CorePlayer) extends intf.Player:
  import Player.*

  def controller: core.Controller = underlying.controller

  def viewWidth: Double = controller.viewSize._1
  def viewHeight: Double = controller.viewSize._2

  def drawView(canvas: dom.HTMLCanvasElement): Unit =
    val rect = Rectangle2D(0, 0, canvas.width, canvas.height)
    val offscren = new dom.OffscreenCanvas(canvas.width, canvas.height)
    val gc = new CanvasWrapper(offscren).getGraphicsContext2D()
    val ctx = new DrawContext(gc, rect)
    controller.drawView(ctx)
    canvas.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]
      .drawImage(offscren.asInstanceOf[dom.HTMLElement], 0, 0)
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

  def keyDown(event: intf.KeyboardEvent): Unit =
    import event.*

    val corePhysicalKey = physicalKeyMap.getOrElse(event.physicalKey, PhysicalKey.Unidentified)

    val coreEvent = KeyEvent(corePhysicalKey, keyString, repeat, shiftDown, controlDown, altDown, metaDown)

    if keyEventCont.isDefined then
      val cont = keyEventCont.get
      keyEventCont = None
      processControlResult(cont(coreEvent))
    else if !playerBusy then
      playerBusy = true
      processControlResult(controller.onKeyEvent(coreEvent))
    end if
  end keyDown
end Player

object Player:
  private val physicalKeyMap: Map[String, PhysicalKey] =
    PhysicalKey.values.map(key => key.toString() -> key).toMap
end Player
