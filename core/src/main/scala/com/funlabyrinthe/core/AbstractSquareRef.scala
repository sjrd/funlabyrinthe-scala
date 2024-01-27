package com.funlabyrinthe.core

abstract class AbstractSquareRef:
  type ThisSquareRefType >: this.type <: AbstractSquareRef
  type ThisMapType <: SquareMap

  val map: ThisMapType
  val pos: Position

  override def toString(): String = s"$map$pos"

  def withMap(map: ThisMapType): ThisSquareRefType
  def withPos(pos: Position): ThisSquareRefType

  def x: Int = pos.x
  def y: Int = pos.y
  def z: Int = pos.z

  def +(x: Int, y: Int, z: Int): ThisSquareRefType = withPos(pos + (x, y, z))
  def +(x: Int, y: Int): ThisSquareRefType = withPos(pos + (x, y))

  def -(x: Int, y: Int, z: Int): ThisSquareRefType = withPos(pos - (x, y, z))
  def -(x: Int, y: Int): ThisSquareRefType = withPos(pos - (x, y))

  def +>(dir: Direction): ThisSquareRefType = withPos(pos +> dir)
  def <+(dir: Direction): ThisSquareRefType = withPos(pos <+ dir)

  def withX(x: Int): ThisSquareRefType = withPos(pos.withX(x))
  def withY(y: Int): ThisSquareRefType = withPos(pos.withY(y))
  def withZ(z: Int): ThisSquareRefType = withPos(pos.withZ(z))

  def isInside: Boolean = map.contains(pos)
  def isOutside: Boolean = !isInside
end AbstractSquareRef
