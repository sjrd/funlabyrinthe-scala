package com.funlabyrinthe.core

import input.KeyEvent

sealed trait ControlResult

object ControlResult {
  case object Done extends ControlResult
  final case class Sleep(ms: Int,
      cont: Unit => ControlResult) extends ControlResult
  final case class WaitForKeyEvent(
      cont: KeyEvent => ControlResult) extends ControlResult
}
