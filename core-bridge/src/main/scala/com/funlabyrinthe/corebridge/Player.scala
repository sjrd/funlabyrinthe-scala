package com.funlabyrinthe.corebridge

import scala.collection.mutable
import scala.scalajs.js
import scala.scalajs.js.typedarray.*

import org.scalajs.dom

import com.funlabyrinthe.core
import com.funlabyrinthe.core.ControlHandler
import com.funlabyrinthe.core.input.{KeyEvent, PhysicalKey}
import com.funlabyrinthe.core.sounds.*

import com.funlabyrinthe.coreinterface as intf

final class Player(universe: Universe, underlying: core.CorePlayer) extends intf.Player:
  import Player.*

  def controller: core.Controller = underlying.controller

  def viewSize(): intf.Size =
    val coreSize = controller.viewSize
    intf.Size(coreSize.width, coreSize.height)

  def presentView(): Int8Array = {
    Errors.protect {
      universe.writeSceneUpdateFragment(controller.present())
    }
  }

  private val unitPromise = js.Promise.resolve[Unit](())

  private var playerBusy: Boolean = false
  private val controlQueue = mutable.Queue[() => Unit]()
  private var keyEventResolver: Option[KeyEvent => Unit] = None
  private val externalEventQueue = mutable.Queue.empty[intf.ExternalEvent]

  private def doAsBusy(op: => Unit): Unit =
    assert(!playerBusy)
    playerBusy = true
    val p = JSPI.async {
      op
    }
    p.`then` { unit =>
      playerBusy = false
      processQueueItem()
    }
  end doAsBusy

  private[corebridge] final def processQueueItem(): Unit =
    Errors.protect {
      if !playerBusy && controlQueue.nonEmpty then
        val op = controlQueue.dequeue()
        doAsBusy(op())
    }
  end processQueueItem

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

    def enqueueUnderControl(op: () => Unit): Unit =
      controlQueue.enqueue(op)
      unitPromise.`then`(_ => processQueueItem())

    def playSound(sound: Sound, volume: Volume, playbackPolicy: PlaybackPolicy): Unit = {
      val playbackPolicy0 = playbackPolicy match
        case PlaybackPolicy.StopAll          => intf.ExternalEvent.PlaySound.PlaybackPolicy.StopAll
        case PlaybackPolicy.StopPreviousSame => intf.ExternalEvent.PlaySound.PlaybackPolicy.StopPreviousSame
        case PlaybackPolicy.Continue         => intf.ExternalEvent.PlaySound.PlaybackPolicy.Continue

      externalEventQueue.enqueue(
          intf.ExternalEvent.PlaySound(sound.assetName, volume.value, playbackPolicy0))
    }
  })

  def keyDown(event: intf.KeyboardEvent): Unit =
    import event.*

    Errors.protect {
      val corePhysicalKey = physicalKeyMap.getOrElse(event.physicalKey, PhysicalKey.Unidentified)

      val coreEvent = KeyEvent(corePhysicalKey, keyString, repeat, shiftDown, controlDown, altDown, metaDown)

      if !playerBusy then
        doAsBusy {
          controller.onKeyEvent(coreEvent)
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

  def popExternalEvent(): js.UndefOr[intf.ExternalEvent] =
    if externalEventQueue.isEmpty then js.undefined
    else externalEventQueue.dequeue()
end Player

object Player:
  private val physicalKeyMap: Map[String, PhysicalKey] =
    PhysicalKey.values.map(key => key.toString() -> key).toMap
end Player
