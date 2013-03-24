package com.funlabyrinthe.core

import scala.reflect.ClassTag

trait Maps { universe: Universe =>
  class DrawSquareContext[A <: AbstractSquare[A]](
      _gc: GraphicsContext, _rect: Rectangle2D,
      val map: Option[Map[A]], val pos: Option[Position])
        extends DrawContext(_gc, _rect) {
    def this(baseContext: DrawContext) =
      this(baseContext.gc, baseContext.rect, None, None)
  }

  class Map[A <: AbstractSquare[A]](_dimensions: Dimensions)(
      implicit squareClassTag: ClassTag[A]) extends Component {
    def this()(implicit squareClassTag: ClassTag[A]) =
      this(Dimensions(0, 0, 0))

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

    private val _map = Array.ofDim[AbstractSquare[_]](dimx, dimy, dimz)
    private val _outside = new Array[AbstractSquare[_]](dimz)

    @inline private def rawContains(x: Int, y: Int, z: Int) =
      x >= 0 && x < dimx && y >= 0 && y < dimy && z >= 0 && z < dimz

    final def contains(x: Int, y: Int, z: Int): Boolean =
      rawContains(x-origx, y-origy, z-origz)

    final def contains(pos: Position): Boolean =
      contains(pos.x, pos.y, pos.z)

    private def rawOutside(z: Int): Square =
      _outside(if (z < 0) 0 else if (z >= dimz) dimz-1 else z).asInstanceOf

    private def rawApply(x: Int, y: Int, z: Int): Square = {
      if (rawContains(x, y, z)) _map(x)(y)(z).asInstanceOf
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
  }

  trait AbstractSquare[A <: AbstractSquare[A]] extends VisualComponent {
    type Square = A

    def drawTo(context: DrawSquareContext[A]) {
      super.drawTo(context:DrawContext)
    }

    override final def drawTo(context: DrawContext) {
      drawTo(new DrawSquareContext[A](context))
    }
  }
}
