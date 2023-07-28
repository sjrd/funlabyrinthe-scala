package com.funlabyrinthe.core

import scala.collection.immutable.ListMap
import scala.reflect.ClassTag

import com.funlabyrinthe.core.pickling.*

abstract class SquareMap(using ComponentInit) extends Component {

  type Square <: AbstractSquare[_]

  protected def squareIsPickleable: Pickleable[Square]

  val SquareWidth = 30.0
  val SquareHeight = 30.0
  final def SquareSize = (SquareWidth, SquareHeight)

  private var dimx = 0
  private var dimy = 0
  private var dimz = 0

  final def dimensions: Dimensions = Dimensions(dimx, dimy, dimz)

  private var _map = new Array[Array[Array[AbstractSquare[_]]]](0)
  private var _outside = new Array[AbstractSquare[_]](0)

  override def save()(using Context): ListMap[String, Pickle] =
    given Pickleable[Square] = squareIsPickleable

    val inherited = super.save()

    val mapPickles = _map.toList.map(_.toList.map(_.toList.map(x => Pickleable.pickle(x.asInstanceOf[Square]))))
    val mapPickle = ListPickle(mapPickles.map(a => ListPickle(a.map(b => ListPickle(b)))))
    val outsidePickle = Pickleable.pickle(_outside.toList.asInstanceOf[List[Square]])

    inherited ++ List(
      "map" -> mapPickle,
      "outside" -> outsidePickle,
    )
  end save

  override def load(pickleFields: Map[String, Pickle])(using Context): Unit =
    given Pickleable[Square] = squareIsPickleable

    super.load(pickleFields)

    for mapPickle <- pickleFields.get("map"); map <- Pickleable.unpickle[List[List[List[Square]]]](mapPickle) do
      if map.isEmpty then
        _map = new Array(0)
        dimx = 0
        dimy = 0
        dimz = 0
        _outside = new Array(0)
      else
        _map = map.map(_.map(_.toArray[AbstractSquare[_]]).toArray).toArray
        dimx = _map.length
        dimy = _map(0).length
        dimz = _map(0)(0).length
        _outside = Array.fill(dimz)(_map(0)(0)(0))
    end for

    for outsidePickle <- pickleFields.get("outside"); outside <- Pickleable.unpickle[List[Square]](outsidePickle) do
      if outside.sizeIs == dimz then
        _outside = outside.toArray[AbstractSquare[_]]
  end load

  def resize(dimensions: Dimensions, fill: Square): Unit = {
    dimx = dimensions.x
    dimy = dimensions.y
    dimz = dimensions.z

    _map = Array.fill[AbstractSquare[_]](dimx, dimy, dimz)(fill)
    _outside = Array.fill[AbstractSquare[_]](dimz)(fill)
  }

  final def contains(x: Int, y: Int, z: Int): Boolean =
    x >= 0 && x < dimx && y >= 0 && y < dimy && z >= 0 && z < dimz

  final def contains(pos: Position): Boolean =
    contains(pos.x, pos.y, pos.z)

  final def outside: SquareMap.OutsideRef[this.type] =
    new SquareMap.OutsideRef(this)

  private final def getOutside(z: Int): Square =
    _outside(if (z < 0) 0 else if (z >= dimz) dimz-1 else z).asInstanceOf[Square]

  private final def setOutside(z: Int, square: Square): Unit =
    if (z >= 0 && z < dimz)
      _outside(z) = square

  final def apply(x: Int, y: Int, z: Int): Square =
    if (contains(x, y, z)) _map(x)(y)(z).asInstanceOf[Square]
    else getOutside(z)

  final def apply(pos: Position): Square =
    apply(pos.x, pos.y, pos.z)

  final def update(x: Int, y: Int, z: Int, square: Square): Unit = {
    if (contains(x, y, z))
      _map(x)(y)(z) = square
  }

  final def update(pos: Position, square: Square): Unit =
    update(pos.x, pos.y, pos.z, square)

  final def ref(pos: Position): SquareRef[this.type] = SquareRef(this, pos)
  final def ref(x: Int, y: Int, z: Int): SquareRef[this.type] =
    ref(Position(x, y, z))

  final def minPos = Position(0, 0, 0)
  final def maxPos = Position(dimx, dimy, dimz)

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
