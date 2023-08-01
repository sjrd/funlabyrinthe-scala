package com.funlabyrinthe.core.graphics

case class Point2D(x: Double, y: Double):
  def +(that: Point2D): Point2D =
    Point2D(this.x + that.x, this.y + that.y)

  def -(that: Point2D): Point2D =
    Point2D(this.x - that.x, this.y - that.y)
end Point2D
