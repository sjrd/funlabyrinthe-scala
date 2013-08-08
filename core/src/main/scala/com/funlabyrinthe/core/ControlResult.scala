package com.funlabyrinthe.core

sealed trait ControlResult

object ControlResult {
  final case object Done extends ControlResult
  final case class Sleep(ms: Int,
      cont: Unit => ControlResult) extends ControlResult
}
