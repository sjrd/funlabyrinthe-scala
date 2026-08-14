package com.funlabyrinthe.coreinterface

import scala.scalajs.js

/** Event from the game to the outside world.
 *
 *  Currently this is only used for sound effects.
 */
trait ExternalEvent extends js.Object {
  val kind: ExternalEvent.ExternalEventKind
}

object ExternalEvent {
  opaque type ExternalEventKind = String

  object ExternalEventKind {
    val PlaySound: ExternalEventKind = "play-sound"
  }

  trait PlaySound extends ExternalEvent {
    val assetName: String

    /** Volume from 0.0 (minimum) to 1.0 (maximum). */
    val volume: Double

    val playbackPolicy: PlaySound.PlaybackPolicy
  }

  object PlaySound {
    opaque type PlaybackPolicy = String

    object PlaybackPolicy {
      /** Stop all sounds, not only the previous same sound. */
      val StopAll: PlaybackPolicy = "stop-all"

      /** Stop only the previous same sound. */
      val StopPreviousSame: PlaybackPolicy = "stop-previous-same"

      /** Continue all previous sounds. */
      val Continue: PlaybackPolicy = "continue"
    }

    def apply(assetName: String, volume: Double, playbackPolicy: PlaybackPolicy): PlaySound = {
      val assetName0 = assetName
      val volume0 = volume
      val playbackPolicy0 = playbackPolicy
      new PlaySound {
        val kind = ExternalEventKind.PlaySound
        val assetName = assetName0
        val volume = volume0
        val playbackPolicy = playbackPolicy0
      }
    }

    def unapply(event: ExternalEvent): Option[(String, Double, PlaybackPolicy)] =
      if event.kind == ExternalEventKind.PlaySound then
        val event1 = event.asInstanceOf[PlaySound]
        Some(event1.assetName, event1.volume, event1.playbackPolicy)
      else
        None
  }
}
