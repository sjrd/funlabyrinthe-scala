package com.funlabyrinthe.core

import scala.collection.immutable.*

import com.funlabyrinthe.core.pickling.Pickleable

final case class Position(x: Int, y: Int, z: Int) derives Pickleable {
  override def toString(): String = s"($x, $y, $z)"

  def +(a: Int, b: Int, c: Int): Position =
    Position(x+a, y+b, z+c)

  def +(a: Int, b: Int): Position =
    Position(x+a, y+b, z)

  def +(that: Position): Position =
    Position(this.x + that.x, this.y + that.y, this.z + that.z)

  def -(a: Int, b: Int, c: Int): Position =
    Position(x-a, y-b, z-c)

  def -(a: Int, b: Int): Position =
    Position(x-a, y-b, z)

  def -(that: Position): Position =
    Position(this.x - that.x, this.y - that.y, this.z - that.z)

  def +>(dir: Direction): Position = dir match {
    case Direction.North => Position(x, y-1, z)
    case Direction.East  => Position(x+1, y, z)
    case Direction.South => Position(x, y+1, z)
    case Direction.West  => Position(x-1, y, z)
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

  val Zero: Position = Position(0, 0, 0)

  final case class Range(xrange: IntRange, yrange: IntRange, zrange: IntRange)
      extends AbstractSeq[Position]
      with IndexedSeq[Position]
      with IndexedSeqOps[Position, IndexedSeq, IndexedSeq[Position]]
      with StrictOptimizedSeqOps[Position, IndexedSeq, IndexedSeq[Position]]
      with scala.collection.IterableFactoryDefaults[Position, IndexedSeq] {

    @inline def start: Position = Position(xrange.start, yrange.start, zrange.start)
    @inline def end: Position = Position(xrange.end, yrange.end, zrange.end)

    // The step is not really a Position (should be a PositionDiff?)
    @inline def step: (Int, Int, Int) = (xrange.step, yrange.step, zrange.step)

    @inline def xspan: Int = xrange.length
    @inline def yspan: Int = yrange.length
    @inline def zspan: Int = zrange.length

    override def length: Int = xspan * yspan * zspan

    override def apply(index: Int): Position = {
      val xspan = this.xspan
      val yspan = this.yspan
      val x = xrange(index % xspan)
      val y = yrange(index / xspan % yspan)
      val z = zrange(index / xspan / yspan)
      Position(x, y, z)
    }

    override def foreach[U](f: Position => U): Unit = {
      for (z <- zrange)
        for (y <- yrange)
          for (x <- xrange)
            f(Position(x, y, z))
    }

    override def contains[A >: Position](elem: A): Boolean = elem match {
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

    def inclusive(start: Position, end: Position) = {
      new Range(
          IntRange.inclusive(start.x, end.x),
          IntRange.inclusive(start.y, end.y),
          IntRange.inclusive(start.z, end.z))
    }

    abstract class Mapped[+A](posRange: Range)
        extends AbstractSeq[A]
        with IndexedSeq[A]
        with IndexedSeqOps[A, IndexedSeq, IndexedSeq[A]]
        with StrictOptimizedSeqOps[A, IndexedSeq, IndexedSeq[A]]
        with scala.collection.IterableFactoryDefaults[A, IndexedSeq]:

      protected def mapper(pos: Position): A

      override def length: Int = posRange.length

      override def apply(i: Int): A = mapper(posRange(i))

      override def foreach[U](f: A => U): Unit =
        posRange.foreach(pos => f(mapper(pos)))
    end Mapped
  }
}
