package com.funlabyrinthe.core.graphics

case class Rectangle2D(minX: Double, minY: Double,
    width: Double, height: Double) {

  val maxX: Double = minX + width
  val maxY: Double = minY + height

  def topLeft: Point2D = Point2D(minX, minY)

  def bottomRight: Point2D = Point2D(maxX, maxY)

  def center: Point2D = Point2D(minX + width / 2, minY + height / 2)

  def contains(x: Double, y: Double): Boolean =
    (x >= minX && y >= minY && x < maxX && y < maxY)

  def contains(pt: Point2D): Boolean = contains(pt.x, pt.y)

  def +(diff: Point2D): Rectangle2D =
    Rectangle2D(minX + diff.x, minY + diff.y, width, height)

  def paddedInner(padding: Insets): Rectangle2D =
    Rectangle2D(
      minX + padding.left,
      minY + padding.top,
      width - padding.left - padding.right,
      height - padding.top - padding.bottom
    )
  end paddedInner
}

object Rectangle2D extends ((Double, Double, Double, Double) => Rectangle2D) {
  val Empty: Rectangle2D = new Rectangle2D(0, 0, 0, 0)

  def fromBounds(minX: Double, minY: Double,
      maxX: Double, maxY: Double): Rectangle2D =
    new Rectangle2D(minX, minY, maxX-minX, maxY-minY)
}
