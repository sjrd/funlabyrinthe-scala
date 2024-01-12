package com.funlabyrinthe.core

import scala.collection.immutable.ListMap
import scala.collection.mutable

import com.funlabyrinthe.core.inspecting.Inspectable
import com.funlabyrinthe.core.pickling.*
import com.funlabyrinthe.core.reflect.*

abstract class SquareMap(using ComponentInit) extends Component derives Reflector {

  type Square <: AbstractSquare[_]

  protected def squareIsPickleable: Pickleable[Square]

  @transient @noinspect
  val SquareWidth = 30.0

  @transient @noinspect
  val SquareHeight = 30.0

  @transient @noinspect
  final def SquareSize = (SquareWidth, SquareHeight)

  private var dimx = 0
  private var dimy = 0
  private var dimz = 0

  @transient @noinspect
  final def dimensions: Dimensions = Dimensions(dimx, dimy, dimz)

  private var _map = new Array[AbstractSquare[_]](0)
  private var _outside = new Array[AbstractSquare[_]](0)

  private def posToIndex(x: Int, y: Int, z: Int): Int =
    x + (dimx * (y + (dimy * z)))

  private def linearMap(index: Int): Square = _map(index).asInstanceOf[Square]

  private object ReflectedMap extends InspectedData:
    val name: String = "map"

    type Value = Unit

    def isPickleable: Boolean = true

    def value: Value = ()

    def storeDefaults(): Unit = ()

    def pickle()(using PicklingContext): Option[Pickle] = Some(pickleMap())

    def unpickle(pickle: Pickle)(using PicklingContext): Unit =
      pickle match
        case ObjectPickle(fields) => unpickleMap(fields.toMap)
        case _                    => ()
    end unpickle

    def inspectable: Option[Inspectable[Unit]] = None
  end ReflectedMap

  override def reflect() = autoReflect[SquareMap]

  override protected def reflectProperties(): List[InspectedData] =
    super.reflectProperties() :+ ReflectedMap

  private def pickleMap()(using PicklingContext): Pickle =
    given Pickleable[Square] = squareIsPickleable

    // Build the palette

    val palette = mutable.LinkedHashMap.empty[Square, Int]
    def register(abstractSquare: AbstractSquare[_]): Unit =
      val square = abstractSquare.asInstanceOf[Square]
      if !palette.contains(square) then
        palette(square) = palette.size
    end register

    _map.foreach(register(_))
    _outside.foreach(register(_))

    // Make the pickles

    val palettePickle = Pickleable.pickle(palette.keysIterator.toList)

    val mapPickle =
      val floors =
        for z <- (0 until dimz).toList yield
          val lines =
            for y <- (0 until dimy).toList yield
              val columns =
                for x <- (0 until dimx).toList yield
                  Pickleable.pickle(palette(linearMap(posToIndex(x, y, z))))
              ListPickle(columns)
          ListPickle(lines)
      ListPickle(floors)

    val outsidePickle = Pickleable.pickle(_outside.toList.map(square => palette(square.asInstanceOf[Square])))

    ObjectPickle(
      List(
        "palette" -> palettePickle,
        "map" -> mapPickle,
        "outside" -> outsidePickle,
      )
    )
  end pickleMap

  private def unpickleMap(pickleFields: Map[String, Pickle])(using PicklingContext): Unit =
    given Pickleable[Square] = squareIsPickleable

    for
      palettePickle <- pickleFields.get("palette")
      mapPickle <- pickleFields.get("map")
      outsidePickle <- pickleFields.get("outside")
      paletteList <- Pickleable.unpickle[List[Square]](palettePickle)
      map <- Pickleable.unpickle[List[List[List[Int]]]](mapPickle)
      outside <- Pickleable.unpickle[List[Int]](outsidePickle)
    do
      if map.isEmpty then
        dimx = 0
        dimy = 0
        dimz = 0
        _map = new Array(0)
        _outside = new Array(0)
      else
        val palette = paletteList.toArray[AbstractSquare[_]]
        val fill = paletteList.head
        resize(Dimensions(map.head.head.size, map.head.size, map.size), fill)

        var index = 0
        for floor <- map; line <- floor; column <- line do
          _map(index) = palette(column)
          index += 1

        _outside = outside.map(palette(_)).toArray
    end for
  end unpickleMap

  def resize(dimensions: Dimensions, fill: Square): Unit = {
    dimx = dimensions.x
    dimy = dimensions.y
    dimz = dimensions.z

    _map = Array.fill[AbstractSquare[_]](dimx * dimy * dimz)(fill)
    _outside = Array.fill[AbstractSquare[_]](dimz)(fill)
  }

  def resizeAndTranslate(dimensions: Dimensions, posOfOldOrigin: Position, fill: Square): Unit =
    val newMap = Array.fill[AbstractSquare[_]](dimensions.x * dimensions.y * dimensions.z)(fill)

    def newIndex(x: Int, y: Int, z: Int): Int =
      x + (dimensions.x * (y + (dimensions.y * z)))

    for newPos <- Position(0, 0, 0) until dimensions.toPosition do
      val oldPos = newPos - posOfOldOrigin
      if contains(oldPos) then
        newMap(newIndex(newPos.x, newPos.y, newPos.z)) = _map(posToIndex(oldPos.x, oldPos.y, oldPos.z))
    end for

    val newOutside = Array.fill[AbstractSquare[_]](dimensions.z)(fill)

    for newZ <- 0 until dimensions.z do
      val oldZ = newZ - posOfOldOrigin.z
      if oldZ >= 0 && oldZ < dimz then
        newOutside(newZ) = _outside(oldZ)
    end for

    dimx = dimensions.x
    dimy = dimensions.y
    dimz = dimensions.z

    _map = newMap
    _outside = newOutside
  end resizeAndTranslate

  final def contains(x: Int, y: Int, z: Int): Boolean =
    x >= 0 && x < dimx && y >= 0 && y < dimy && z >= 0 && z < dimz

  final def contains(pos: Position): Boolean =
    contains(pos.x, pos.y, pos.z)

  @transient @noinspect
  final def outside: SquareMap.OutsideRef[this.type] =
    new SquareMap.OutsideRef(this)

  private final def getOutside(z: Int): Square =
    _outside(if (z < 0) 0 else if (z >= dimz) dimz-1 else z).asInstanceOf[Square]

  private final def setOutside(z: Int, square: Square): Unit =
    if (z >= 0 && z < dimz)
      _outside(z) = square

  final def apply(x: Int, y: Int, z: Int): Square =
    if (contains(x, y, z)) linearMap(posToIndex(x, y, z))
    else getOutside(z)

  final def apply(pos: Position): Square =
    apply(pos.x, pos.y, pos.z)

  final def update(x: Int, y: Int, z: Int, square: Square): Unit = {
    if (contains(x, y, z))
      _map(posToIndex(x, y, z)) = square
  }

  final def update(pos: Position, square: Square): Unit =
    update(pos.x, pos.y, pos.z, square)

  final def ref(pos: Position): SquareRef[this.type] = SquareRef(this, pos)
  final def ref(x: Int, y: Int, z: Int): SquareRef[this.type] =
    ref(Position(x, y, z))

  @transient @noinspect
  final def minPos = Position(0, 0, 0)
  @transient @noinspect
  final def maxPos = Position(dimx, dimy, dimz)

  @transient @noinspect
  final def minRef = SquareRef[this.type](this, minPos)
  @transient @noinspect
  final def maxRef = SquareRef[this.type](this, maxPos)

  @transient @noinspect
  final def allPositions: Position.Range = minPos until maxPos
  @transient @noinspect
  final def allRefs: SquareRef.Range[this.type] = minRef until maxRef
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
