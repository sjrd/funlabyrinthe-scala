package com.funlabyrinthe.core

final case class Position(x: Int, y: Int, z: Int) {
  def +(a: Int, b: Int, c: Int): Position =
    Position(x+a, y+b, z+c)

  def +(a: Int, b: Int): Position =
    Position(x+a, y+b, z)

  def -(a: Int, b: Int, c: Int): Position =
    Position(x-a, y-b, z-c)

  def -(a: Int, b: Int): Position =
    Position(x-a, y-b, z)

  def +>(dir: Direction): Position = dir match {
    case North => Position(x, y-1, z)
    case East  => Position(x+1, y, z)
    case South => Position(x, y+1, z)
    case West  => Position(x-1, y, z)
  }

  def <+(dir: Direction): Position = this +> dir.opposite

  def withX(x: Int): Position = copy(x = x)
  def withY(y: Int): Position = copy(y = y)
  def withZ(z: Int): Position = copy(z = z)

  def to(that: Position) = Position.Range.inclusive(this, that)
  def until(that: Position) = Position.Range(this, that)

  def until_+(a: Int, b: Int) =
    new Position.Range(x until (x+a), y until (y+b), z to z)

  def until_+(a: Int, b: Int, c: Int) =
    new Position.Range(x until (x+a), y until (y+b), z until (z+c))
}

object Position {
  import scala.collection.immutable._
  import scala.{ Range => IntRange }

  final case class Range(xrange: IntRange, yrange: IntRange, zrange: IntRange)
  extends Iterable[Position]
     with Seq[Position]
     with IndexedSeq[Position] {

    @inline def start = Position(xrange.start, yrange.start, zrange.start)
    @inline def end = Position(xrange.end, yrange.end, zrange.end)

    // The step is not really a Position (should be a PositionDiff?)
    @inline def step = (xrange.step, yrange.step, zrange.step)

    @inline def xspan = xrange.length
    @inline def yspan = yrange.length
    @inline def zspan = zrange.length

    override def length = xspan * yspan * zspan

    override def apply(index: Int): Position = {
      val xspan = this.xspan
      val yspan = this.yspan
      val x = xrange(index % xspan)
      val y = yrange(index / xspan % yspan)
      val z = zrange(index / xspan / yspan)
      Position(x, y, z)
    }

    override def foreach[U](f: Position => U) {
      for (z <- zrange)
        for (y <- yrange)
          for (x <- xrange)
            f(Position(x, y, z))
    }

    override def contains[A >: Position](elem: A) = elem match {
      case pos: Position =>
        (xrange.contains(pos.x) && yrange.contains(pos.y) &&
            zrange.contains(pos.z))
      case _ =>
        false
    }

    def by(stepx: Int, stepy: Int, stepz: Int) =
      new Range(xrange by stepx, yrange by stepy, zrange by stepz)

    def by(stepx: Int, stepy: Int) =
      new Range(xrange by stepx, yrange by stepy, zrange)
  }

  object Range {
    def apply(start: Position, end: Position) = {
      new Range(
          IntRange(start.x, end.x),
          IntRange(start.y, end.y),
          IntRange(start.z, end.z))
    }

    def apply(start: Position, end: Position,
        stepx: Int, stepy: Int, stepz: Int) = {
      new Range(
          IntRange(start.x, end.x, stepx),
          IntRange(start.y, end.y, stepy),
          IntRange(start.z, end.z, stepz))
    }

    def inclusive(start: Position, end: Position) = {
      new Range(
          IntRange.inclusive(start.x, end.x),
          IntRange.inclusive(start.y, end.y),
          IntRange.inclusive(start.z, end.z))
    }

    def inclusive(start: Position, end: Position,
        stepx: Int, stepy: Int, stepz: Int) = {
      new Range(
          IntRange.inclusive(start.x, end.x, stepx),
          IntRange.inclusive(start.y, end.y, stepy),
          IntRange.inclusive(start.z, end.z, stepz))
    }
  }
}
