package com.funlabyrinthe.core

import com.funlabyrinthe.core.input.KeyEvent
import com.funlabyrinthe.core.sounds.*

private[funlabyrinthe] trait ControlHandler:
  def sleep(ms: Int): Unit

  def waitForKeyEvent(): KeyEvent

  def enqueueUnderControl(op: () => Unit): Unit

  def playSound(sound: Sound, volume: Volume, playbackPolicy: PlaybackPolicy): Unit
end ControlHandler

private[funlabyrinthe] object ControlHandler:
  object Uninitialized extends ControlHandler:
    def sleep(ms: Int): Unit =
      throw new IllegalStateException("ControlHandler uninitialized for sleep")

    def waitForKeyEvent(): KeyEvent =
      throw new IllegalStateException("ControlHandler uninitialized for waitForKeyEvent")

    def enqueueUnderControl(op: () => Unit): Unit =
      throw new IllegalStateException("ControlHandler uninitialized for enqueueUnderControl")

    def playSound(sound: Sound, volume: Volume, playbackPolicy: PlaybackPolicy): Unit =
      throw new IllegalStateException("ControlHandler uninitialized for playSound")
  end Uninitialized
end ControlHandler
