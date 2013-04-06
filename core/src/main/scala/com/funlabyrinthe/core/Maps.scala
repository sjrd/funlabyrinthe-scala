package com.funlabyrinthe.core

import scala.language.implicitConversions

import scala.reflect.ClassTag

trait Maps { universe: Universe =>
  class DrawSquareContext[A <: AbstractSquare[A]](
      _gc: GraphicsContext, _rect: Rectangle2D,
      val where: Option[SquareRef[A]]) extends DrawContext(_gc, _rect) {
    def this(baseContext: DrawContext) =
      this(baseContext.gc, baseContext.rect, None)

    @inline final def isNowhere = where.isEmpty
    @inline final def isSomewhere = where.isDefined

    @inline final def map: Option[Map[A]] =
      if (isNowhere) None else Some(where.get.map)

    @inline final def pos: Option[Position] =
      if (isNowhere) None else Some(where.get.pos)
  }

  class Map[A <: AbstractSquare[A]](_dimensions: Dimensions, _initSquare: A)(
      implicit squareClassTag: ClassTag[A]) extends Component {

    type Square = A

    private var dimx = _dimensions.x
    private var dimy = _dimensions.y
    private var dimz = _dimensions.z

    final def dimensions: Dimensions = Dimensions(dimx, dimy, dimz)

    private var origx = 0
    private var origy = 0
    private var origz = 0

    final def origin: Position = Position(origx, origy, origz)
    final def origin_=(pos: Position) {
      origx = pos.x
      origy = pos.y
      origz = pos.z
    }

    private val _map = Array.fill[AbstractSquare[_]](dimx, dimy, dimz)(_initSquare)
    private val _outside = Array.fill[AbstractSquare[_]](dimz)(_initSquare)

    @inline private def rawContains(x: Int, y: Int, z: Int) =
      x >= 0 && x < dimx && y >= 0 && y < dimy && z >= 0 && z < dimz

    final def contains(x: Int, y: Int, z: Int): Boolean =
      rawContains(x-origx, y-origy, z-origz)

    final def contains(pos: Position): Boolean =
      contains(pos.x, pos.y, pos.z)

    private def rawOutside(z: Int): Square =
      _outside(if (z < 0) 0 else if (z >= dimz) dimz-1 else z).asInstanceOf[Square]

    private def rawApply(x: Int, y: Int, z: Int): Square = {
      if (rawContains(x, y, z)) _map(x)(y)(z).asInstanceOf[Square]
      else rawOutside(z)
    }

    private def rawOutsideUpdate(z: Int, square: Square) {
      if (z >= 0 && z < dimz)
        _outside(z) = square
    }

    private def rawUpdate(x: Int, y: Int, z: Int, square: Square) {
      if (rawContains(x, y, z))
        _map(x)(y)(z) = square
    }

    final def outside(z: Int): Square =
      rawOutside(z-origz)

    final def apply(x: Int, y: Int, z: Int): Square =
      rawApply(x-origx, y-origy, z-origz)

    final def apply(pos: Position): Square =
      apply(pos.x, pos.y, pos.z)

    final def update(x: Int, y: Int, z: Int, square: Square) {
      rawUpdate(x-origx, y-origy, z-origz, square)
    }

    final def update(pos: Position, square: Square): Unit =
      update(pos.x, pos.y, pos.z, square)

    final def minPos = Position(-origx, -origy, -origz)
    final def maxPos = Position(dimx-origx, dimy-origy, dimz-origz)

    final def minRef = SquareRefMod(this, minPos)
    final def maxRef = SquareRefMod(this, maxPos)
  }

  type SquareRef[A <: AbstractSquare[A]] = Maps.SquareRef[this.type, A]
  object SquareRefMod {
    def apply[A <: AbstractSquare[A]](map: Map[A], pos: Position) =
      Maps.SquareRef[Maps.this.type, A](map, pos)

    def unapply[A <: AbstractSquare[A]](ref: SquareRef[A]) =
      Some((ref.map, ref.pos))
  }

  trait AbstractSquare[A <: AbstractSquare[A]] extends VisualComponent {
    type Square = A
    type Map = universe.Map[A]

    def drawTo(context: DrawSquareContext[A]) {
      super.drawTo(context:DrawContext)
    }

    override final def drawTo(context: DrawContext) {
      drawTo(new DrawSquareContext[A](context))
    }
  }
}

object Maps {
  import scala.collection.TraversableLike
  import scala.collection.immutable.Traversable

  final case class SquareRef[U <: Universe, A <: U#AbstractSquare[A]](
      map: U#Map[A], pos: Position) {

    def apply(): map.Square = map(pos)
    def update(square: map.Square): Unit = map(pos) = square

    def x = pos.x
    def y = pos.y
    def z = pos.z

    def +(x: Int, y: Int, z: Int) = SquareRef[U, A](map, pos + (x, y, z))
    def +(x: Int, y: Int) = SquareRef[U, A](map, pos + (x, y))

    def -(x: Int, y: Int, z: Int) = SquareRef[U, A](map, pos - (x, y, z))
    def -(x: Int, y: Int) = SquareRef[U, A](map, pos - (x, y))

    def +>(dir: Direction): SquareRef[U, A] = SquareRef(map, pos +> dir)
    def <+(dir: Direction): SquareRef[U, A] = SquareRef(map, pos <+ dir)

    def to(that: SquareRef[U, A]) = SquareRef.Range.inclusive(this, that)
    def until(that: SquareRef[U, A]) = SquareRef.Range(this, that)

    def until_+(a: Int, b: Int) =
      new SquareRef.Range(map, pos until_+ (a, b))

    def until_+(a: Int, b: Int, c: Int) =
      new SquareRef.Range(map, pos until_+ (a, b, c))
  }

  object SquareRef {
    import scala.collection.immutable._
    import Position.{ Range => PosRange }

    @inline implicit def toPosition(ref: SquareRef[_, _]): Position = ref.pos

    final case class Range[U <: Universe, A <: U#AbstractSquare[A]](
        map: U#Map[A], posrange: PosRange)
    extends Iterable[SquareRef[U, A]]
       with Seq[SquareRef[U, A]]
       with IndexedSeq[SquareRef[U, A]] {

      override def length = posrange.length

      override def apply(index: Int): SquareRef[U, A] = {
        SquareRef[U, A](map, posrange(index))
      }

      override def foreach[V](f: SquareRef[U, A] => V) {
        for (pos <- posrange)
          f(SquareRef[U, A](map, pos))
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

      def apply[U <: Universe, A <: U#AbstractSquare[A]](
          start: SquareRef[U, A], end: SquareRef[U, A]) = {
        requireSameMap(start.map, end.map)
        new Range(start.map,
            PosRange(start.pos, end.pos))
      }

      def apply[U <: Universe, A <: U#AbstractSquare[A]](
          start: SquareRef[U, A], end: SquareRef[U, A],
          stepx: Int, stepy: Int, stepz: Int) = {
        requireSameMap(start.map, end.map)
        new Range(start.map,
            PosRange(start.pos, end.pos, stepx, stepy, stepz))
      }

      def inclusive[U <: Universe, A <: U#AbstractSquare[A]](
          start: SquareRef[U, A], end: SquareRef[U, A]) = {
        requireSameMap(start.map, end.map)
        new Range(start.map,
            PosRange.inclusive(start.pos, end.pos))
      }

      def inclusive[U <: Universe, A <: U#AbstractSquare[A]](
          start: SquareRef[U, A], end: SquareRef[U, A],
          stepx: Int, stepy: Int, stepz: Int) = {
        requireSameMap(start.map, end.map)
        new Range(start.map,
            PosRange.inclusive(start.pos, end.pos, stepx, stepy, stepz))
      }
    }
  }
}
