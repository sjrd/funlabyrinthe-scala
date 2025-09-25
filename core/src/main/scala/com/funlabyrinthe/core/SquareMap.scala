package com.funlabyrinthe.core

import scala.collection.immutable.ListMap
import scala.collection.mutable

import com.funlabyrinthe.core.inspecting.Inspectable
import com.funlabyrinthe.core.pickling.*
import com.funlabyrinthe.core.reflect.*

abstract class SquareMap(using ComponentInit) extends Component {

  type Square <: AnyRef

  icon += "Creators/Map"

  category = ComponentCategory("maps", "Maps")

  protected def squareIsPickleable: Pickleable[Square]

  @transient @noinspect
  val SquareWidth: Int = 30

  @transient @noinspect
  val SquareHeight: Int = 30

  @transient @noinspect
  final def SquareSize: (Int, Int) = (SquareWidth, SquareHeight)

  private var dimx = 0
  private var dimy = 0
  private var dimz = 0

  @transient @noinspect
  final def dimensions: Dimensions = Dimensions(dimx, dimy, dimz)

  @transient @noinspect
  def defaultSquare: Square

  private var _map = new Array[AnyRef](0)
  private var _outside = new Array[AnyRef](0)

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
        case ObjectPickle(fields) =>
          unpickleMap(fields.toMap)
        case _ =>
          PicklingContext.typeError("object", pickle)
    end unpickle

    def prepareRemoveReferences(reference: Component, actions: InPlacePickleable.PreparedActions)(
        using PicklingContext): Unit =
      prepareRemoveReferencesFromMap(reference, actions)

    def inspectable: Option[Inspectable[Unit]] = None
  end ReflectedMap

  override protected def reflectProperties(registerData: InspectedData => Unit): Unit =
    super.reflectProperties(registerData)
    Reflectable.autoReflectProperties(this, registerData)
    registerData(ReflectedMap)

  private def pickleMap()(using PicklingContext): Pickle =
    given Pickleable[Square] = squareIsPickleable

    // Build the palette

    val palette = mutable.LinkedHashMap.empty[Square, Int]
    def register(abstractSquare: AnyRef): Unit =
      val square = abstractSquare.asInstanceOf[Square]
      if !palette.contains(square) then
        palette(square) = palette.size
    end register

    _map.foreach(register(_))
    _outside.foreach(register(_))

    // Make the pickles

    val dimensionsPickle = Pickleable.pickle(dimensions)

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
        "dimensions" -> dimensionsPickle,
        "palette" -> palettePickle,
        "map" -> mapPickle,
        "outside" -> outsidePickle,
      )
    )
  end pickleMap

  private def unpickleMap(pickleFields: Map[String, Pickle])(using PicklingContext): Unit =
    given Pickleable[Square] = squareIsPickleable

    def withRequiredField[A >: None.type](fieldName: String)(body: Pickle => A): A =
      pickleFields.get(fieldName) match
        case None =>
          PicklingContext.error(s"missing required field '$fieldName'")
        case Some(fieldPickle) =>
          summon[PicklingContext].withSubPath(fieldName)(body(fieldPickle))

    val dimensions1 = withRequiredField("dimensions") { dimensionsPickle =>
      Pickleable.unpickle[Dimensions](dimensionsPickle).filter { dims =>
        val valid = (dims.x > 0 && dims.y > 0 && dims.z > 0) || dims == Dimensions(0, 0, 0)
        if !valid then
          PicklingContext.reportError(s"invalid dimensions: $dims")
        valid
      }
    }
    val dimensions2 = dimensions1.orElse {
      // Fallback during the transition
      pickleFields.get("map") match
        case Some(ListPickle(Nil)) =>
          Some(Dimensions(0, 0, 0))
        case Some(ListPickle(floors @ (ListPickle(rows @ (ListPickle(columns @ (_ :: _)) :: _)) :: _))) =>
          Some(Dimensions(columns.size, rows.size, floors.size))
        case _ =>
          None
    }

    // At this point, if we couldn't determine valid dimensions, abort
    if dimensions2.isEmpty then
      return
    val dimensions = dimensions2.get

    resize(dimensions, defaultSquare)

    val palette = withRequiredField("palette") { palettePickle =>
      palettePickle match
        case ListPickle(elemPickles) =>
          /* Unlike Pickleable[List[T]].unpickle, which drops elements that
           * cannot be unpickled, we replace them by the defaultSquare.
           * This way, indexing is preserved for valid elements of the palette.
           */
          val elems = elemPickles.map { elemPickle =>
            summon[Pickleable[Square]].unpickle(elemPickle).getOrElse(defaultSquare)
          }
          Some(elems.toArray[AnyRef])
        case _ =>
          PicklingContext.typeError("list", palettePickle)
    }
    val paletteIndices = 0 until palette.fold(0)(_.length)

    def unpicklePaletteRef(indexPickle: Pickle): Square =
      indexPickle match
        case IntegerPickle.ofInt(index) if paletteIndices.contains(index) =>
          // If we get here, the palette must exist, because the range is non-empty
          palette.get(index).asInstanceOf[Square]
        case _: IntegerPickle if palette.isEmpty =>
          // If it is a valid integer and the palette is empty, no point in reporting an error
          defaultSquare
        case _ =>
          PicklingContext.typeError(s"integer in the range $paletteIndices", indexPickle)
          defaultSquare

    def expectListOfSize(tpe: String, expected: Int, pickle: Pickle): List[Pickle] =
      pickle match
        case ListPickle(elems) =>
          if elems.sizeIs == expected then
            elems
          else
            PicklingContext.reportError(s"$tpe count mismatch; expected $expected but got ${elems.size}")
            elems.take(expected)
        case _ =>
          PicklingContext.typeError(s"list with $expected elements", pickle)
          Nil

    withRequiredField("map") { mapPickle =>
      for (floorPickle, floor) <- expectListOfSize("floor", dimensions.z, mapPickle).zipWithIndex do
        summon[PicklingContext].withSubPath(s"floor $floor") {
          for (rowPickle, row) <- expectListOfSize("row", dimensions.y, floorPickle).zipWithIndex do
            for (squarePickle, column) <- expectListOfSize("square", dimensions.x, rowPickle).zipWithIndex do
              _map(posToIndex(column, row, floor)) = unpicklePaletteRef(squarePickle)
        }
    }

    withRequiredField("outside") { outsidePickle =>
      for (elemPickle, floor) <- expectListOfSize("floor", dimensions.z, outsidePickle).zipWithIndex do
        _outside(floor) = unpicklePaletteRef(elemPickle)
    }
  end unpickleMap

  private def prepareRemoveReferencesFromMap(reference: Component, actions: InPlacePickleable.PreparedActions)(
      using PicklingContext): Unit =

    for index <- 0 until _map.length do
      squareIsPickleable.removeReferences(linearMap(index), reference) match
        case Pickleable.RemoveRefResult.Unchanged =>
          () // nothing to do
        case Pickleable.RemoveRefResult.Changed(newValue) =>
          actions.prepare {
            _map(index) = newValue
          }
        case Pickleable.RemoveRefResult.Failure =>
          actions.prepare {
            _map(index) = defaultSquare
          }
    end for

    for z <- 0 until dimz do
      squareIsPickleable.removeReferences(getOutside(z), reference) match
        case Pickleable.RemoveRefResult.Unchanged =>
          () // nothing to do
        case Pickleable.RemoveRefResult.Changed(newValue) =>
          actions.prepare {
            _outside(z) = newValue
          }
        case Pickleable.RemoveRefResult.Failure =>
          actions.prepare {
            _outside(z) = defaultSquare
          }
    end for
  end prepareRemoveReferencesFromMap

  def resize(dimensions: Dimensions, fill: Square): Unit = {
    dimx = dimensions.x
    dimy = dimensions.y
    dimz = dimensions.z

    _map = Array.fill[AnyRef](dimx * dimy * dimz)(fill)
    _outside = Array.fill[AnyRef](dimz)(fill)
  }

  def resizeAndTranslate(dimensions: Dimensions, posOfOldOrigin: Position, fill: Square): Unit =
    val newMap = Array.fill[AnyRef](dimensions.x * dimensions.y * dimensions.z)(fill)

    def newIndex(x: Int, y: Int, z: Int): Int =
      x + (dimensions.x * (y + (dimensions.y * z)))

    for newPos <- Position(0, 0, 0) until dimensions.toPosition do
      val oldPos = newPos - posOfOldOrigin
      if contains(oldPos) then
        newMap(newIndex(newPos.x, newPos.y, newPos.z)) = _map(posToIndex(oldPos.x, oldPos.y, oldPos.z))
    end for

    val newOutside = Array.fill[AnyRef](dimensions.z)(fill)

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

  @transient @noinspect
  final def minPos = Position(0, 0, 0)
  @transient @noinspect
  final def maxPos = Position(dimx, dimy, dimz)

  @transient @noinspect
  final def allPositions: Position.Range = minPos until maxPos
}

object SquareMap {
  class OutsideRef[A <: SquareMap](val map: A) extends AnyVal {
    @inline final def apply(z: Int): map.Square =
      map.getOutside(z)
    @inline final def update(z: Int, square: map.Square): Unit =
      map.setOutside(z, square)
  }
}
