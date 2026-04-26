package com.funlabyrinthe.core.graphics

case class Insets(top: Int, right: Int, bottom: Int, left: Int):
  def topLeft: Point2D = Point2D(left, top)
end Insets

object Insets {
  val Empty: Insets = new Insets(0, 0, 0, 0)
}
