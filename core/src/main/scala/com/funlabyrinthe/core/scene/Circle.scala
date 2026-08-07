package com.funlabyrinthe.core.scene

final case class Circle(center: Point, radius: Int) {
  def x: Int = center.x
  def y: Int = center.y
  def diameter: Int = radius * 2

  def left: Int = x - radius
  def right: Int = x + radius
  def top: Int = y - radius
  def bottom: Int = y + radius

  def contains(vertex: Point): Boolean =
    vertex.distanceTo(center) <= radius
  def contains(x: Int, y: Int): Boolean =
    contains(Point(x, y))

  def +(d: Int): Circle = resize(radius + d)
  def -(d: Int): Circle = resize(radius - d)
  def *(d: Int): Circle = resize(radius * d)
  def /(d: Int): Circle = resize(radius / d)

  def moveBy(amount: Point): Circle =
    this.copy(center = center + amount)
  def moveBy(x: Int, y: Int): Circle =
    moveBy(Point(x, y))

  def moveTo(newCenter: Point): Circle =
    this.copy(center = newCenter)
  def moveTo(x: Int, y: Int): Circle =
    moveTo(Point(x, y))

  def resize(newRadius: Int): Circle =
    this.copy(radius = newRadius)
  def resizeTo(newRadius: Int): Circle =
    resize(newRadius)
  def resizeBy(amount: Int): Circle =
    expand(amount)
  def withRadius(newRadius: Int): Circle =
    resize(newRadius)

  def expand(by: Int): Circle =
    resize(radius + by)
  def contract(by: Int): Circle =
    resize(radius - by)
}

object Circle {
  val unit: Circle =
    Circle(Point.zero, 1)

  def apply(x: Int, y: Int, radius: Int): Circle =
    Circle(Point(x, y), radius)

  def fromTwoPoints(center: Point, boundary: Point): Circle =
    Circle(center, center.distanceTo(boundary).toInt)

  def fromPointCloud(points: Batch[Point]): Circle =
    val bb = Rectangle.fromPointCloud(points)
    Circle(bb.center, bb.center.distanceTo(bb.topLeft).toInt)
}
