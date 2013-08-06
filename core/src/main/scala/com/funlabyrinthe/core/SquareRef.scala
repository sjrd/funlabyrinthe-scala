package com.funlabyrinthe.core

import scala.language.implicitConversions

import scala.collection.TraversableLike
import scala.collection.immutable.Traversable

final case class SquareRef[+M <: SquareMap](map: M, pos: Position) {

  type Map = M
  type Square = Map#Square

  def apply(): map.Square = map(pos)
  def update(square: map.Square): Unit = map(pos) = square

  def x = pos.x
  def y = pos.y
  def z = pos.z

  def +(x: Int, y: Int, z: Int) = SquareRef[Map](map, pos + (x, y, z))
  def +(x: Int, y: Int) = SquareRef[Map](map, pos + (x, y))

  def -(x: Int, y: Int, z: Int) = SquareRef[Map](map, pos - (x, y, z))
  def -(x: Int, y: Int) = SquareRef[Map](map, pos - (x, y))

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

  def until_+(a: Int, b: Int) =
    new SquareRef.Range(map, pos until_+ (a, b))

  def until_+(a: Int, b: Int, c: Int) =
    new SquareRef.Range(map, pos until_+ (a, b, c))
}

object SquareRef {
  import scala.collection.immutable._
  import Position.{ Range => PosRange }

  @inline implicit def toPosition(ref: SquareRef[_]): Position = ref.pos

  final case class Range[+M <: SquareMap](map: M, posrange: PosRange)
  extends Iterable[SquareRef[M]]
     with Seq[SquareRef[M]]
     with IndexedSeq[SquareRef[M]] {

    type Map = M

    override def length = posrange.length

    override def apply(index: Int): SquareRef[Map] = {
      SquareRef[Map](map, posrange(index))
    }

    override def foreach[V](f: SquareRef[Map] => V) {
      for (pos <- posrange)
        f(SquareRef[Map](map, pos))
    }

    override def contains(elem: Any) = elem match {
      case ref: SquareRef[_] =>
        ref.map == map && posrange.contains(ref.pos)
      case _ =>
        false
    }

    def by(stepx: Int, stepy: Int, stepz: Int) =
      new Range(map, posrange by (stepx, stepy, stepz))

    def by(stepx: Int, stepy: Int) =
      new Range(map, posrange by (stepx, stepy))
  }

  object Range {
    private def requireSameMap(map1: AnyRef, map2: AnyRef) {
      require(map1 == map2,
          "Cannot create a range of positions on different maps")
    }

    def apply[M <: SquareMap](
        start: SquareRef[M], end: SquareRef[M]) = {
      requireSameMap(start.map, end.map)
      new Range(start.map,
          PosRange(start.pos, end.pos))
    }

    def apply[M <: SquareMap](
        start: SquareRef[M], end: SquareRef[M],
        stepx: Int, stepy: Int, stepz: Int) = {
      requireSameMap(start.map, end.map)
      new Range(start.map,
          PosRange(start.pos, end.pos, stepx, stepy, stepz))
    }

    def inclusive[M <: SquareMap](
        start: SquareRef[M], end: SquareRef[M]) = {
      requireSameMap(start.map, end.map)
      new Range(start.map,
          PosRange.inclusive(start.pos, end.pos))
    }

    def inclusive[M <: SquareMap](
        start: SquareRef[M], end: SquareRef[M],
        stepx: Int, stepy: Int, stepz: Int) = {
      requireSameMap(start.map, end.map)
      new Range(start.map,
          PosRange.inclusive(start.pos, end.pos, stepx, stepy, stepz))
    }
  }
}
