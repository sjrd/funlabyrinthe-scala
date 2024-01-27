package com.funlabyrinthe.core

import scala.annotation.unchecked.uncheckedVariance

import scala.collection.immutable.Traversable

final case class SquareRef[+M <: SquareMap](map: M, pos: Position) {

  type Map = (M @uncheckedVariance)

  def apply(): map.Square = map(pos)
  def update(square: map.Square): Unit = map(pos) = square

  def x: Int = pos.x
  def y: Int = pos.y
  def z: Int = pos.z

  def +(x: Int, y: Int, z: Int): SquareRef[Map] = SquareRef[Map](map, pos + (x, y, z))
  def +(x: Int, y: Int) = SquareRef[Map](map, pos + (x, y))

  def -(x: Int, y: Int, z: Int): SquareRef[Map] = SquareRef[Map](map, pos - (x, y, z))
  def -(x: Int, y: Int): SquareRef[Map] = SquareRef[Map](map, pos - (x, y))

  def +>(dir: Direction): SquareRef[Map] = SquareRef(map, pos +> dir)
  def <+(dir: Direction): SquareRef[Map] = SquareRef(map, pos <+ dir)

  def withX(x: Int): SquareRef[Map] = copy(pos = pos.withX(x))
  def withY(y: Int): SquareRef[Map] = copy(pos = pos.withY(y))
  def withZ(z: Int): SquareRef[Map] = copy(pos = pos.withZ(z))
  def withMap[A <: SquareMap](map: A): SquareRef[A] = copy[A](map = map)

  def to[A >: Map <: SquareMap](that: SquareRef[A]): SquareRef.Range[A] =
    SquareRef.Range.inclusive(this, that)
  def until[A >: Map <: SquareMap](that: SquareRef[A]): SquareRef.Range[A] =
    SquareRef.Range(this, that)

  def until_+(a: Int, b: Int): SquareRef.Range[Map] =
    new SquareRef.Range(map, pos until_+ (a, b))

  def until_+(a: Int, b: Int, c: Int): SquareRef.Range[Map] =
    new SquareRef.Range(map, pos until_+ (a, b, c))

  def isInside: Boolean = map.contains(pos)
  def isOutside: Boolean = !isInside
}

object SquareRef {
  import scala.collection.immutable._
  import Position.{ Range => PosRange }

  final class Range[+M <: SquareMap](val map: M, val posRange: PosRange)
      extends PosRange.Mapped[SquareRef[M]](posRange):

    protected def mapper(pos: Position): SquareRef[M] =
      SquareRef(map, pos)

    override def contains[A >: SquareRef[M]](elem: A): Boolean = elem match {
      case ref: SquareRef[_] =>
        ref.map == map && posRange.contains(ref.pos)
      case _ =>
        false
    }

    def by(stepx: Int, stepy: Int, stepz: Int): Range[M] =
      new Range(map, posRange by (stepx, stepy, stepz))

    def by(stepx: Int, stepy: Int): Range[M] =
      new Range(map, posRange by (stepx, stepy))
  end Range

  object Range {
    private def requireSameMap(map1: AnyRef, map2: AnyRef): Unit = {
      require(map1 == map2,
          "Cannot create a range of positions on different maps")
    }

    def apply[M <: SquareMap](map: M, posRange: PosRange): Range[M] =
      new Range(map, posRange)

    def apply[M <: SquareMap](start: SquareRef[M], end: SquareRef[M]): Range[M] =
      requireSameMap(start.map, end.map)
      new Range(start.map, PosRange(start.pos, end.pos))

    def inclusive[M <: SquareMap](start: SquareRef[M], end: SquareRef[M]): Range[M] =
      requireSameMap(start.map, end.map)
      new Range(start.map, PosRange.inclusive(start.pos, end.pos))
  }
}
