package com.funlabyrinthe.core.scene

import com.funlabyrinthe.core.pickling.Pickleable

final case class Size(width: Int, height: Int) derives Pickleable {
  def +(size: Size): Size = Size(width + size.width, height + size.height)
  def +(i: Int): Size = Size(width + i, height + i)
  def +(d: Double): Size = Size((width.toDouble + d).toInt, (height.toDouble + d).toInt)
  def -(size: Size): Size = Size(width - size.width, height - size.height)
  def -(i: Int): Size = Size(width - i, height - i)
  def -(d: Double): Size = Size((width.toDouble - d).toInt, (height.toDouble - d).toInt)

  def *(i: Int): Size = Size(width * i, height * i)
  def *(d: Double): Size = Size((width.toDouble * d).toInt, (height.toDouble * d).toInt)
  def /(i: Int): Size = Size(width / i, height / i)
  def /(d: Double): Size = Size((width.toDouble / d).toInt, (height.toDouble / d).toInt)

  def withWidth(newX: Int): Size = this.copy(width = newX)
  def withHeight(newY: Int): Size = this.copy(height = newY)

  def toPoint: Point =
    Point(width, height)

  def centerPoint: Point =
    Point(width >> 1, height >> 1)
}

object Size {
  def apply(xy: Int): Size =
    Size(xy, xy)

  val zero: Size = Size(0, 0)
  val one: Size = Size(1, 1)
}
