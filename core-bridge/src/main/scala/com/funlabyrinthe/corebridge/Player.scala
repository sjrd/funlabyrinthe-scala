package com.funlabyrinthe.corebridge

import scala.scalajs.js

import org.scalajs.dom

import com.funlabyrinthe.core
import com.funlabyrinthe.core.ControlHandler
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
    Errors.protect {
      val rect = Rectangle2D(0, 0, canvas.width, canvas.height)
      val offscren = new dom.OffscreenCanvas(canvas.width, canvas.height)
      val gc = new CanvasWrapper(offscren, 0).getGraphicsContext2D()
      val ctx = new DrawContext(gc, tickCount = underlying.universe.tickCount, rect)
      controller.drawView(ctx)
      canvas.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]
        .drawImage(offscren.asInstanceOf[dom.HTMLElement], 0, 0)
    }
  end drawView

  private var playerBusy: Boolean = false
  private var keyEventResolver: Option[KeyEvent => Unit] = None

  underlying.setControlHandler(new ControlHandler {
    def sleep(ms: Int): Unit =
      if !playerBusy || keyEventResolver.isDefined then
        throw new IllegalStateException("No ongoing action for sleep")

      if ms > 0 then
        val p = js.Promise[Unit]({ (resolve, reject) =>
          js.timers.setTimeout(ms) {
            resolve(())
          }
        })
        JSPI.await(p)
      end if
    end sleep

    def waitForKeyEvent(): KeyEvent =
      if !playerBusy || keyEventResolver.isDefined then
        throw new IllegalStateException("No ongoing action for waitForKeyEvent")

      val p = js.Promise[KeyEvent]({ (resolve, reject) =>
        keyEventResolver = Some(event => resolve(event))
      })
      JSPI.await(p)
    end waitForKeyEvent
  })

  def keyDown(event: intf.KeyboardEvent): Unit =
    import event.*

    Errors.protect {
      val corePhysicalKey = physicalKeyMap.getOrElse(event.physicalKey, PhysicalKey.Unidentified)

      val coreEvent = KeyEvent(corePhysicalKey, keyString, repeat, shiftDown, controlDown, altDown, metaDown)

      if !playerBusy then
        playerBusy = true
        val p = JSPI.async {
          controller.onKeyEvent(coreEvent)
        }
        p.`then` { unit =>
          playerBusy = false
        }
      else
        keyEventResolver match
          case Some(resolver) =>
            keyEventResolver = None
            resolver(coreEvent)
          case None =>
            ()
      end if
    }
  end keyDown
end Player

object Player:
  private val physicalKeyMap: Map[String, PhysicalKey] =
    PhysicalKey.values.map(key => key.toString() -> key).toMap
end Player
