package com.funlabyrinthe.core.scene

import com.funlabyrinthe.core.pickling.Pickleable

final case class Point(x: Int, y: Int) derives Pickleable {
  def +(pt: Point): Point = Point(x + pt.x, y + pt.y)
  def +(i: Int): Point    = Point(x + i, y + i)
  def +(d: Double): Point = Point((x.toDouble + d).toInt, (y.toDouble + d).toInt)
  def -(pt: Point): Point = Point(x - pt.x, y - pt.y)
  def -(i: Int): Point    = Point(x - i, y - i)
  def -(d: Double): Point = Point((x.toDouble - d).toInt, (y.toDouble - d).toInt)
  def *(pt: Point): Point = Point(x * pt.x, y * pt.y)
  def *(i: Int): Point    = Point(x * i, y * i)
  def *(d: Double): Point = Point((x.toDouble * d).toInt, (y.toDouble * d).toInt)
  def /(pt: Point): Point = Point(x / pt.x, y / pt.y)
  def /(i: Int): Point    = Point(x / i, y / i)
  def /(d: Double): Point = Point((x.toDouble / d).toInt, (y.toDouble / d).toInt)

  def withX(newX: Int): Point = this.copy(x = newX)
  def withY(newY: Int): Point = this.copy(y = newY)

  def abs: Point =
    Point(Math.abs(x), Math.abs(y))

  def invert: Point =
    Point(-x, -y)

  def moveTo(newPosition: Point): Point =
    newPosition
  def moveTo(x: Int, y: Int): Point =
    moveTo(Point(x, y))

  def moveBy(amount: Point): Point =
    this + amount
  def moveBy(x: Int, y: Int): Point =
    moveBy(Point(x, y))

  def distanceTo(other: Point): Double =
    Point.distanceBetween(this, other)

  def toSize: Size =
    Size(x, y)
}

object Point {
  def apply(xy: Int): Point =
    Point(xy, xy)

  val zero: Point = Point(0, 0)
  val one: Point = Point(1, 1)

  def distanceBetween(a: Point, b: Point): Double = {
    (a, b) match
      case (Point(x1, y1), Point(x2, y2)) if x1 == x2 =>
        Math.abs((y2 - y1).toDouble)

      case (Point(x1, y1), Point(x2, y2)) if y1 == y2 =>
        Math.abs((x2 - x1).toDouble)

      case (Point(x1, y1), Point(x2, y2)) =>
        val aa = x2.toDouble - x1.toDouble
        val bb = y2.toDouble - y1.toDouble
        Math.sqrt(Math.abs((aa * aa) + (bb * bb)))
  }
}
