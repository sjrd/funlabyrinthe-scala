package com.funlabyrinthe.core.graphics

sealed abstract class TextAlignment

object TextAlignment {
  case object Left extends TextAlignment
  case object Center extends TextAlignment
  case object Right extends TextAlignment
}
