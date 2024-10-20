package com.funlabyrinthe.mazes

import com.funlabyrinthe.core.*
import com.funlabyrinthe.core.pickling.Pickleable

final case class SquareRef(map: Map, pos: Position) extends AbstractSquareRef derives Pickleable:
  type ThisSquareRefType = SquareRef
  type ThisMapType = Map

  def apply(): Square = map(pos)
  def update(square: Square): Unit = map(pos) = square

  def withMap(map: Map): SquareRef = copy(map = map)
  def withPos(pos: Position): SquareRef = copy(pos = pos)

  infix def to(that: SquareRef): SquareRef.Range =
    SquareRef.Range.inclusive(this, that)
  infix def until(that: SquareRef): SquareRef.Range =
    SquareRef.Range.exclusive(this, that)

  infix def until_+(a: Int, b: Int): SquareRef.Range =
    new SquareRef.Range(map, pos until_+ (a, b))

  infix def until_+(a: Int, b: Int, c: Int): SquareRef.Range =
    new SquareRef.Range(map, pos until_+ (a, b, c))
end SquareRef

object SquareRef:
  import Position.{ Range => PosRange }

  final class Range(val map: Map, val posRange: PosRange)
      extends PosRange.Mapped[SquareRef](posRange):

    protected def mapper(pos: Position): SquareRef =
      SquareRef(map, pos)

    override def contains[A >: SquareRef](elem: A): Boolean = elem match
      case ref: SquareRef =>
        ref.map == map && posRange.contains(ref.pos)
      case _ =>
        false
    end contains

    def by(stepx: Int, stepy: Int, stepz: Int): Range =
      new Range(map, posRange by (stepx, stepy, stepz))

    def by(stepx: Int, stepy: Int): Range =
      new Range(map, posRange by (stepx, stepy))
  end Range

  object Range:
    def apply(map: Map, posRange: PosRange): Range =
      new Range(map, posRange)

    def exclusive(start: SquareRef, end: SquareRef): Range =
      new Range(start.map, PosRange(start.pos, end.pos))

    def inclusive(start: SquareRef, end: SquareRef): Range =
      new Range(start.map, PosRange.inclusive(start.pos, end.pos))
  end Range
end SquareRef
