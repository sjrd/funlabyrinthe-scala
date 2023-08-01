package com.funlabyrinthe.core.graphics

case class Insets(top: Double, right: Double, bottom: Double, left: Double):
  def topLeft: Point2D = Point2D(left, top)
end Insets

object Insets extends ((Double, Double, Double, Double) => Insets) {
  val Empty: Insets = new Insets(0, 0, 0, 0)
}
