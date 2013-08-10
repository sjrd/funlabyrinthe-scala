package com.funlabyrinthe.core.graphics

case class Rectangle2D(minX: Double, minY: Double,
    width: Double, height: Double) {

  val maxX: Double = minX + width
  val maxY: Double = minY + height

  def contains(x: Double, y: Double): Boolean =
    (x >= minX && y >= minY && x < maxX && y < maxY)

  def contains(pt: Point2D): Boolean = contains(pt.x, pt.y)
}

object Rectangle2D extends ((Double, Double, Double, Double) => Rectangle2D) {
  val Empty: Rectangle2D = new Rectangle2D(0, 0, 0, 0)

  def fromBounds(minX: Double, minY: Double,
      maxX: Double, maxY: Double): Rectangle2D =
    new Rectangle2D(minX, minY, maxX-minX, maxY-minY)
}
