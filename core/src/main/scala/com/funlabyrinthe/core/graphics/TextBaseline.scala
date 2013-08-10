package com.funlabyrinthe.core.graphics

sealed abstract class TextBaseline

object TextBaseline {
  case object Top extends TextBaseline
  case object Middle extends TextBaseline
  case object Alphabetic extends TextBaseline
  case object Bottom extends TextBaseline
}
