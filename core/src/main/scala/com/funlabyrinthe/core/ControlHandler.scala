package com.funlabyrinthe.core

import com.funlabyrinthe.core.input.KeyEvent

private[funlabyrinthe] trait ControlHandler:
  def sleep(ms: Int): Unit

  def waitForKeyEvent(): KeyEvent

  def enqueueUnderControl(op: () => Unit): Unit
end ControlHandler

private[funlabyrinthe] object ControlHandler:
  object Uninitialized extends ControlHandler:
    def sleep(ms: Int): Unit =
      throw new IllegalStateException("ControlHandler uninitialized for sleep")

    def waitForKeyEvent(): KeyEvent =
      throw new IllegalStateException("ControlHandler uninitialized for waitForKeyEvent")

    def enqueueUnderControl(op: () => Unit): Unit =
      throw new IllegalStateException("ControlHandler uninitialized for enqueueUnderControl")
  end Uninitialized
end ControlHandler
