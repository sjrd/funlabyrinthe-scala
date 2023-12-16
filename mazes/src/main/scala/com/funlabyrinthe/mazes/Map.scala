package com.funlabyrinthe.mazes

import com.funlabyrinthe.core._
import com.funlabyrinthe.core.graphics._
import com.funlabyrinthe.core.input._
import com.funlabyrinthe.core.pickling.*
import com.funlabyrinthe.core.MapEditInterface.ResizingView

final class Map(using ComponentInit) extends SquareMap with EditableMap {
  type Square = com.funlabyrinthe.mazes.Square

  private var _zoneSize = (7, 7)

  final def zoneWidth: Int = _zoneSize._1
  final def zoneHeight: Int = _zoneSize._2
  final def zoneSize = _zoneSize

  def defaultSquare: Square = Mazes.mazes.Grass

  protected def squareIsPickleable: Pickleable[Square] = summon[Pickleable[Square]]

  final def posComponentsBottomUp(pos: Position): List[PosComponent] =
    val ref = Some(SquareRef(this, pos))
    Mazes.mazes.posComponentsBottomUp.filter(_.position == ref)

  final def posComponentsTopDown(pos: Position): List[PosComponent] =
    val ref = Some(SquareRef(this, pos))
    Mazes.mazes.posComponentsTopDown.filter(_.position == ref)

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
      for posComponent <- Mazes.mazes.posComponentsBottomUp do
        posComponent.position match
          case Some(SquareRef(this, pos)) =>
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

    def getFloorRect(floor: Int): Rectangle2D =
      new Rectangle2D(0, 0, (dimensions.x+2)*SquareWidth,
          (dimensions.y+2)*SquareWidth)

    def drawFloor(context: DrawContext, floor: Int): Unit =
      drawMapContent(context, floor)
      drawZoneLimits(context, floor)
    end drawFloor

    private def drawMapContent(context: DrawContext, floor: Int): Unit =
      val min = minRef.withZ(floor) - (1, 1)
      val max = maxRef.withZ(floor)

      for (ref <- min to max) {
        val x = (ref.x - min.x) * SquareWidth
        val y = (ref.y - min.y) * SquareHeight

        val rect = new Rectangle2D(context.minX+x, context.minY+y,
            SquareWidth, SquareHeight)

        val squareContext = new DrawSquareContext[Map](
            context.gc, rect, Some(ref))
        ref().drawTo(squareContext)

        for posComponent <- map.posComponentsBottomUp(ref.pos) do
          posComponent.drawTo(squareContext)
      }
    end drawMapContent

    private def drawZoneLimits(context: DrawContext, floor: Int): Unit =
      drawZoneLimitsCommon(context, dimensions, SquareWidth, SquareHeight, zoneWidth, zoneHeight)

    def getDescriptionAt(x: Double, y: Double, floor: Int): String =
      getPosAt(x, y, floor) map (p => map(p).toString()) getOrElse ""

    override def onMouseClicked(event: MouseEvent, floor: Int,
        component: Component): Unit = {
      getPosAt(event.x, event.y, floor) match {
        case Some(pos) =>
          component match
            case component: PosComponent =>
              component.position = Some(ref(pos))
            case _ =>
              updatePosition(pos, component)
        case None =>
          ()
      }
    }

    def updatePosition(pos: Position, component: Component): Unit = {
      if (map.contains(pos)) {
        // Inside
        val ref = SquareRef(map, pos)

        component match {
          case field: Field =>
            ref() = field
          case effect: Effect =>
            ref() = ref().field + effect
          case tool: Tool =>
            ref() = ref().field + ref().effect + tool
          case obstacle: Obstacle =>
            ref() = ref() + obstacle

          case _ =>
            ()
        }
      } else {
        // Outside
        component match {
          case field: Field =>
            map.outside(pos.z) = field

          case _ =>
            ()
        }
      }
    }

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

    def getFloorRect(floor: Int): Rectangle2D =
      new Rectangle2D(0, 0, (dimensions.x + 2) * SquareWidth, (dimensions.y + 2) * SquareWidth)

    def drawFloor(context: DrawContext, floor: Int): Unit =
      drawMapContent(context, floor)
      drawZoneLimits(context, floor)
    end drawFloor

    private def drawMapContent(context: DrawContext, floor: Int): Unit =
      val min = Position(-1, -1, floor)
      val max = Position(dimensions.x, dimensions.y, floor)

      for pos <- min to max do
        val x = (pos.x - min.x) * SquareWidth
        val y = (pos.y - min.y) * SquareHeight

        val rect = new Rectangle2D(context.minX + x, context.minY + y, SquareWidth, SquareHeight)

        myPosToOldPos(pos) match
          case Some(oldPos) =>
            val ref = map.ref(oldPos)
            val squareContext = new DrawSquareContext[Map](context.gc, rect, Some(ref))
            ref().drawTo(squareContext)

            for posComponent <- map.posComponentsBottomUp(oldPos) do
              posComponent.drawTo(squareContext)

          case None =>
            val squareContext = new DrawSquareContext[Map](context.gc, rect, None)
            map.defaultSquare.drawTo(squareContext)
        end match
      end for
    end drawMapContent

    private def drawZoneLimits(context: DrawContext, floor: Int): Unit =
      drawZoneLimitsCommon(context, dimensions, SquareWidth, SquareHeight, zoneWidth, zoneHeight)

    def getDescriptionAt(x: Double, y: Double, floor: Int): String =
      getPosAt(x, y, floor) match
        case Some(myPos) =>
          val square = myPosToOldPos(myPos).fold(map.defaultSquare)(oldPos => map(oldPos))
          square.toString()
        case None =>
          ""
    end getDescriptionAt

    override def onMouseClicked(event: MouseEvent, floor: Int, component: Component): Unit =
      ()

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

  private def drawZoneLimitsCommon(
    context: DrawContext,
    dims: Dimensions,
    squareWidth: Double,
    squareHeight: Double,
    zoneWidth: Int,
    zoneHeight: Int,
  ): Unit =
    import context.*

    gc.fill = Color.Black

    for x <- 0 to dims.x by zoneWidth do
      gc.fillRect(squareWidth + (x * squareWidth) - 1, 0, 3, rect.height)

    for y <- 0 to dims.y by zoneHeight do
      gc.fillRect(0, squareHeight + (y * squareHeight) - 1, rect.width, 3)
  end drawZoneLimitsCommon
}
