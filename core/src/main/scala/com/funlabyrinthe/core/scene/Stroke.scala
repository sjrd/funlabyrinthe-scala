package com.funlabyrinthe.core.scene

final case class Stroke(width: Int, color: RGBA) {
  def withWidth(newWidth: Int): Stroke =
    this.copy(width = newWidth)

  def withColor(newColor: RGBA): Stroke =
    this.copy(color = newColor)
}

object Stroke {
  val Black: Stroke =
    Stroke(1, RGBA.Black)

  val None: Stroke =
    Stroke(0, RGBA.Transparent)

  def apply(color: RGBA): Stroke =
    Stroke(1, color)

  def apply(width: Int): Stroke =
    Stroke(width, RGBA.Black)
}
