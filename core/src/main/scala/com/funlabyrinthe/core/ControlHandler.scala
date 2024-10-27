package com.funlabyrinthe.core

import com.funlabyrinthe.core.input.KeyEvent

private[funlabyrinthe] trait ControlHandler:
  def sleep(ms: Int): Unit

  def waitForKeyEvent(): KeyEvent
end ControlHandler

private[funlabyrinthe] object ControlHandler:
  object Uninitialized extends ControlHandler:
    def sleep(ms: Int): Unit =
      throw new IllegalStateException("ControlHandler uninitialized for sleep")

    def waitForKeyEvent(): KeyEvent =
      throw new IllegalStateException("ControlHandler uninitialized for waitForKeyEvent")
  end Uninitialized
end ControlHandler
