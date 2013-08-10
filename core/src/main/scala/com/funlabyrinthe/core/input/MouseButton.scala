package com.funlabyrinthe.core.input

abstract sealed class MouseButton

object MouseButton {
  case object None extends MouseButton
  case object Primary extends MouseButton
  case object Middle extends MouseButton
  case object Secondary extends MouseButton
}
