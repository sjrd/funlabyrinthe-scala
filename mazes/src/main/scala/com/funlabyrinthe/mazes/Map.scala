package com.funlabyrinthe.mazes

import com.funlabyrinthe.core._
import com.funlabyrinthe.core.input._
import com.funlabyrinthe.core.pickling.*
import com.funlabyrinthe.core.scene.*
import com.funlabyrinthe.core.MapEditInterface.ResizingView

final class Map(using ComponentInit) extends SquareMap with EditableMap {
  type Square = com.funlabyrinthe.mazes.Square

  icon += "Maps/MazeMap"

  private var _zoneSize = (7, 7)

  final def zoneWidth: Int = _zoneSize._1
  final def zoneWidth_=(value: Int): Unit =
    require(value >= 1, s"Illegal zone width: $value")
    _zoneSize = (value, _zoneSize._2)

  final def zoneHeight: Int = _zoneSize._2
  final def zoneHeight_=(value: Int): Unit =
    require(value >= 1, s"Illegal zone height: $value")
    _zoneSize = (_zoneSize._1, value)

  @transient @noinspect
  final def zoneSize = _zoneSize

  @transient @noinspect
  def defaultSquare: Square = grass

  protected def squareIsPickleable: Pickleable[Square] = summon[Pickleable[Square]]

  final def ref(pos: Position): SquareRef = SquareRef(this, pos)
  final def ref(x: Int, y: Int, z: Int): SquareRef =
    ref(Position(x, y, z))

  @transient @noinspect
  final def minRef: SquareRef = SquareRef(this, minPos)
  @transient @noinspect
  final def maxRef: SquareRef = SquareRef(this, maxPos)

  @transient @noinspect
  final def allRefs: SquareRef.Range = minRef until maxRef

  final def posComponentsBottomUp(pos: Position): List[PosComponent] =
    val ref = Some(SquareRef(this, pos))
    Mazes.posComponentsBottomUp.filter(_.position == ref)

  final def posComponentsTopDown(pos: Position): List[PosComponent] =
    val ref = Some(SquareRef(this, pos))
    Mazes.posComponentsTopDown.filter(_.position == ref)

  final def playersBottomUp(pos: Position): List[Player] =
    posComponentsBottomUp(pos).collect {
      case p: Player => p
    }

  final def playersTopDown(pos: Position): List[Player] =
    posComponentsTopDown(pos).collect {
      case p: Player => p
    }

  def presentSquare(pos: Position, drawPurpose: DrawPurpose): Batch[SceneNode] = {
    val cellSize = Size(SquareWidth, SquareHeight)
    val squareContext = PresentSquareContext(universe.tickCount, Some(ref(pos)), drawPurpose, cellSize)
    val square = this(pos)

    var result = square.present(squareContext)
    for posComponent <- posComponentsBottomUp(pos) do
      result ++= posComponent.present(squareContext)
    result ++= square.presentCeiling(squareContext)

    result
  }

  override def getEditInterface(): MapEditInterface =
    new Map.EditInterface(this)

  private def commitResize(
    newZoneSize: (Int, Int),
    newDimensions: Dimensions,
    posOfOldOrigin: Position,
  ): Unit =
    _zoneSize = newZoneSize

    resizeAndTranslate(newDimensions, posOfOldOrigin, defaultSquare)

    if posOfOldOrigin != Position.Zero then
      for posComponent <- Mazes.posComponentsBottomUp do
        posComponent.position match
          case Some(SquareRef(map, pos)) if map == this =>
            posComponent.position = Some(SquareRef(this, pos + posOfOldOrigin))
          case _ =>
            ()
    end if
  end commitResize
}

object Map {
  private class EditInterface(val map: Map) extends MapEditInterface {
    import map._

    def floors: Int = dimensions.z

    def getFloorSize(floor: Int): Size =
      Size((dimensions.x+2)*SquareWidth, (dimensions.y+2)*SquareWidth)

    def presentFloor(floor: Int): SceneUpdateFragment =
      SceneUpdateFragment(presentMapContent(floor) ++ presentZoneLimits(floor))

    private def presentMapContent(floor: Int): Batch[Layer] = {
      val min = minPos.withZ(floor) - (1, 1)
      val max = maxPos.withZ(floor)
      val drawPurpose = DrawPurpose.EditMap(map, floor)

      val nodes = (min to max).map { pos =>
        Batch(
          Group(presentSquare(pos, drawPurpose))
            .moveBy((pos.x - min.x) * SquareWidth + (SquareWidth / 2), (pos.y - min.y) * SquareHeight + (SquareHeight / 2))
        )
      }.reduceLeft(_ ++ _)

      Batch(Layer(nodes))
    }

    private def presentZoneLimits(floor: Int): Batch[Layer] =
      presentZoneLimitsCommon(dimensions, SquareWidth, SquareHeight, zoneWidth, zoneHeight)

    def getDescriptionAt(x: Double, y: Double, floor: Int): String =
      getPosAt(x, y, floor) match
        case Some(pos) => makeDescriptionString(pos, map(pos))
        case None      => ""
    end getDescriptionAt

    override def onMouseClicked(event: MouseEvent, floor: Int, component: Component)(
        using EditingServices): Unit =
      for pos <- getPosAt(event.x, event.y, floor) do
        component match
          case component: MapEditingHooksComponent =>
            MapEditingHooksComponent.onEditMouseClickOnMap(component, event, map.ref(pos))
          case component: ItemDef =>
            tryPutItemDefOnMap(event, component, map.ref(pos))
          case _ =>
            ()
    end onMouseClicked

    private def tryPutItemDefOnMap(event: MouseEvent, itemDef: ItemDef, pos: SquareRef)(
        using EditingServices): Unit =
      universe.components[ItemTool].filter(_.item.contains(itemDef)) match
        case tool :: Nil =>
          MapEditingHooksComponent.onEditMouseClickOnMap(tool, event, pos)
        case _ =>
          EditingServices.error(
            s"Cannot put the item definition $itemDef on a map. "
              + "Did you mean to select a tool instead?"
          )
    end tryPutItemDefOnMap

    private def getPosAt(x: Double, y: Double,
        floor: Int): Option[Position] = {

      val squareX = Math.floor(x / SquareWidth).toInt - 1
      val squareY = Math.floor(y / SquareHeight).toInt - 1

      if (squareX >= -1 && squareX <= dimensions.x &&
          squareY >= -1 && squareY <= dimensions.y)
        Some(map.minPos + (squareX, squareY, floor))
      else
        None
    }

    def newResizingView(): ResizingView =
      new ResizingInterface(map)
  }

  private class ResizingInterface(val map: Map) extends MapEditInterface.ResizingView:
    val SquareWidth = map.SquareWidth
    val SquareHeight = map.SquareHeight

    var zoneWidth = map.zoneWidth
    var zoneHeight = map.zoneHeight

    var dimensions: Dimensions = map.dimensions
    var posOfOldOrigin: Position = Position(0, 0, 0)

    final def minPos = Position(0, 0, 0)
    final def maxPos = Position(dimensions.x, dimensions.y, dimensions.z)

    private def myPosToOldPos(pos: Position): Option[Position] =
      val result = pos - posOfOldOrigin
      if map.contains(result) then Some(result)
      else None

    def floors: Int = dimensions.z

    def getFloorSize(floor: Int): Size =
      Size((dimensions.x + 2) * SquareWidth, (dimensions.y + 2) * SquareWidth)

    def presentFloor(floor: Int): SceneUpdateFragment =
      SceneUpdateFragment(presentMapContent(floor) ++ presentZoneLimits(floor))

    private def presentMapContent(floor: Int): Batch[Layer] = {
      val min = Position(-1, -1, floor)
      val max = Position(dimensions.x, dimensions.y, floor)
      val drawPurpose = DrawPurpose.EditMap(map, floor)

      val nodes = (min to max).map { pos =>
        val squareNodes = myPosToOldPos(pos) match {
          case Some(oldPos) =>
            map.presentSquare(pos, drawPurpose)
          case None =>
            val squareContext =
              PresentSquareContext(map.universe.tickCount, None, drawPurpose, Size(SquareWidth, SquareHeight))
            map.defaultSquare.present(squareContext)
        }

        Batch(
          Group(squareNodes)
            .moveBy((pos.x - min.x) * SquareWidth + (SquareWidth / 2), (pos.y - min.y) * SquareHeight + (SquareHeight / 2))
        )
      }.reduceLeft(_ ++ _)

      Batch(Layer(nodes))
    }

    private def presentZoneLimits(floor: Int): Batch[Layer] =
      presentZoneLimitsCommon(dimensions, SquareWidth, SquareHeight, zoneWidth, zoneHeight)

    def getDescriptionAt(x: Double, y: Double, floor: Int): String =
      getPosAt(x, y, floor) match
        case Some(myPos) =>
          val square = myPosToOldPos(myPos).fold(map.defaultSquare)(oldPos => map(oldPos))
          makeDescriptionString(myPos, square)
        case None =>
          ""
    end getDescriptionAt

    override def onMouseClicked(event: MouseEvent, floor: Int, component: Component)(
        using EditingServices): Unit =
      () // ignore
    end onMouseClicked

    private def getPosAt(x: Double, y: Double,
        floor: Int): Option[Position] = {

      val squareX = Math.floor(x / SquareWidth).toInt - 1
      val squareY = Math.floor(y / SquareHeight).toInt - 1

      if (squareX >= -1 && squareX <= dimensions.x &&
          squareY >= -1 && squareY <= dimensions.y)
        Some(map.minPos + (squareX, squareY, floor))
      else
        None
    }

    def newResizingView(): ResizingView =
      new ResizingInterface(map)

    def canResize(direction: Direction3D, grow: Boolean): Boolean =
      if grow then
        true
      else
        direction match
          case Direction3D.North | Direction3D.South =>
            dimensions.y > zoneHeight
          case Direction3D.East | Direction3D.West =>
            dimensions.x > zoneWidth
          case Direction3D.Up | Direction3D.Down =>
            dimensions.z > 1
    end canResize

    def resize(direction: Direction3D, grow: Boolean): Unit =
      def adaptDimension(value: Int, step: Int): Int =
        if grow then (value / step + 1) * step
        else (value - 1) / step * step

      def changeDimsOnly(newDimensions: Dimensions): Unit =
        dimensions = newDimensions

      def changeDimsAndTranslate(newDimensions: Dimensions): Unit =
        posOfOldOrigin += (newDimensions.toPosition - dimensions.toPosition)
        dimensions = newDimensions

      direction match
        case Direction3D.North =>
          changeDimsAndTranslate(dimensions.withY(adaptDimension(dimensions.y, zoneHeight)))
        case Direction3D.East =>
          changeDimsOnly(dimensions.withX(adaptDimension(dimensions.x, zoneWidth)))
        case Direction3D.South =>
          changeDimsOnly(dimensions.withY(adaptDimension(dimensions.y, zoneHeight)))
        case Direction3D.West =>
          changeDimsAndTranslate(dimensions.withX(adaptDimension(dimensions.x, zoneWidth)))
        case Direction3D.Up =>
          changeDimsOnly(dimensions.withZ(adaptDimension(dimensions.z, 1)))
        case Direction3D.Down =>
          changeDimsAndTranslate(dimensions.withZ(adaptDimension(dimensions.z, 1)))
      end match
    end resize

    def commit(): Unit =
      map.commitResize((zoneWidth, zoneHeight), dimensions, posOfOldOrigin)
  end ResizingInterface

  private def makeDescriptionString(pos: Position, square: Square): String =
    s"$pos\u2003$square"

  private def presentZoneLimitsCommon(
    dims: Dimensions,
    squareWidth: Int,
    squareHeight: Int,
    zoneWidth: Int,
    zoneHeight: Int,
  ): Batch[Layer] = {
    val fill = Fill.Color(RGBA.Black)

    val verticalSize = Size(3, (dims.y + 2) * squareHeight)
    val vertical =
      for x <- 0 to dims.x by zoneWidth yield
        Shape.Box(Rectangle(Point((1 + x) * squareWidth - 1, 0), verticalSize), fill, Stroke.None)

    val horizontalSize = Size((dims.x + 2) * squareWidth, 3)
    val horizontal =
      for y <- 0 to dims.y by zoneHeight yield
        Shape.Box(Rectangle(Point(0, (1 + y) * squareHeight - 1), horizontalSize), fill, Stroke.None)

    Batch(Layer(Batch.from(vertical ++ horizontal)))
  }
}
