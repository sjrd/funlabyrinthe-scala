package com.funlabyrinthe.core

import scala.reflect.ClassTag

trait SquareMap extends Component {

  type Square <: AbstractSquare[_]

  val SquareWidth = 30.0
  val SquareHeight = 30.0
  final def SquareSize = (SquareWidth, SquareHeight)

  private var dimx = 0
  private var dimy = 0
  private var dimz = 0

  final def dimensions: Dimensions = Dimensions(dimx, dimy, dimz)

  private var origx = 0
  private var origy = 0
  private var origz = 0

  final def origin: Position = Position(origx, origy, origz)
  final def origin_=(pos: Position): Unit = {
    origx = pos.x
    origy = pos.y
    origz = pos.z
  }

  private var _map = new Array[Array[Array[AbstractSquare[_]]]](0)
  private var _outside = new Array[AbstractSquare[_]](0)

  protected def resize(dimensions: Dimensions, fill: Square): Unit = {
    dimx = dimensions.x
    dimy = dimensions.y
    dimz = dimensions.z

    _map = Array.fill[AbstractSquare[_]](dimx, dimy, dimz)(fill)
    _outside = Array.fill[AbstractSquare[_]](dimz)(fill)
  }

  protected def resize(dimensions: Dimensions, origin: Position, fill: Square): Unit = {
    resize(dimensions, fill)
    this.origin = origin
  }

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

  private def rawOutsideUpdate(z: Int, square: Square): Unit = {
    if (z >= 0 && z < dimz)
      _outside(z) = square
  }

  private def rawUpdate(x: Int, y: Int, z: Int, square: Square): Unit = {
    if (rawContains(x, y, z))
      _map(x)(y)(z) = square
  }

  final def outside: SquareMap.OutsideRef[this.type] =
    new SquareMap.OutsideRef(this)

  private final def getOutside(z: Int): Square =
    rawOutside(z-origz)
  private final def setOutside(z: Int, square: Square): Unit =
    rawOutsideUpdate(z-origz, square)

  final def apply(x: Int, y: Int, z: Int): Square =
    rawApply(x-origx, y-origy, z-origz)

  final def apply(pos: Position): Square =
    apply(pos.x, pos.y, pos.z)

  final def update(x: Int, y: Int, z: Int, square: Square): Unit = {
    rawUpdate(x-origx, y-origy, z-origz, square)
  }

  final def update(pos: Position, square: Square): Unit =
    update(pos.x, pos.y, pos.z, square)

  final def ref(pos: Position): SquareRef[this.type] = SquareRef(this, pos)
  final def ref(x: Int, y: Int, z: Int): SquareRef[this.type] =
    ref(Position(x, y, z))

  final def minPos = Position(origx, origy, origz)
  final def maxPos = Position(dimx+origx, dimy+origy, dimz+origz)

  final def minRef = SquareRef[this.type](this, minPos)
  final def maxRef = SquareRef[this.type](this, maxPos)
}

object SquareMap {
  class OutsideRef[A <: SquareMap](val map: A) extends AnyVal {
    @inline final def apply(z: Int): map.Square =
      map.getOutside(z)
    @inline final def update(z: Int, square: map.Square): Unit =
      map.setOutside(z, square)
  }
}

trait AbstractSquare[A <: AbstractSquare[A]] {
  type Square = A
  type Map <: SquareMap { type Square = A }

  def drawTo(context: DrawSquareContext[Map]): Unit
}
