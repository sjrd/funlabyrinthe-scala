package com.funlabyrinthe.core.scene

import com.funlabyrinthe.core.pickling.Pickleable

final case class Rectangle(topLeft: Point, size: Size) derives Pickleable {
  def left: Int = topLeft.x
  def top: Int = topLeft.y
  def width: Int = size.width
  def height: Int = size.height

  def right: Int = left + width
  def bottom: Int = top + height

  def topRight: Point = Point(right, top)
  def bottomLeft: Point = Point(left, bottom)
  def bottomRight: Point = Point(right, bottom)

  def horizontalCenter: Int = left + (width >> 1)
  def verticalCenter: Int = top + (height >> 1)
  def center: Point = Point(horizontalCenter, verticalCenter)

  def halfSize: Size = size / 2

  def contains(pt: Point): Boolean =
    contains(pt.x, pt.y)
  def contains(x: Int, y: Int): Boolean =
    x >= left && x < right && y >= top && y < bottom

  def moveBy(point: Point): Rectangle =
    this.copy(topLeft = topLeft + point)
  def moveBy(x: Int, y: Int): Rectangle =
    moveBy(Point(x, y))

  def moveTopLeftTo(point: Point): Rectangle =
    this.copy(topLeft = point)
  def moveTopLeftTo(x: Int, y: Int): Rectangle =
    moveTopLeftTo(Point(x, y))

  def expand(amount: Int): Rectangle =
    Rectangle(topLeft - amount, size + (2 * amount))
  def expand(amount: Size): Rectangle =
    Rectangle.ltwh(left - amount.width, top - amount.width, width + 2 * amount.width, height + 2 * amount.height)
}

object Rectangle {
  /** Constructs a rectangle from its left, top, width and height. */
  def ltwh(x: Int, y: Int, width: Int, height: Int): Rectangle =
    Rectangle(Point(x, y), Size(width, height))

  /** Constructs a rectangle from its center, width and height. */
  def cwh(center: Point, width: Int, height: Int): Rectangle =
    Rectangle(center - Point(width / 2, height / 2), Size(width, height))

  /** Constructs a rectangle of given width and height, whose top-left corner is at `(0, 0)`. */
  def sized(width: Int, height: Int): Rectangle =
    Rectangle(Point.zero, Size(width, height))

  /** Constructs a rectangle of given size, whose top-left corner is at `(0, 0)`. */
  def sized(size: Size): Rectangle =
    Rectangle(Point.zero, size)

  private[scene] def fromPointCloud(points: Batch[Point]): Rectangle = {
    var left = Int.MaxValue
    var top = Int.MaxValue
    var right = Int.MinValue
    var bottom = Int.MinValue

    for point <- points.toIndexedSeq do
      left = Math.min(left, point.x)
      top = Math.min(top, point.y)
      right = Math.max(right, point.x)
      bottom = Math.max(bottom, point.y)

    Rectangle.ltwh(left, top, right - left, bottom - top)
  }
}
