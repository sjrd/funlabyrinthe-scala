package com.funlabyrinthe.mazes

import com.funlabyrinthe.core._
import com.funlabyrinthe.core.graphics._
import com.funlabyrinthe.core.input._
import com.funlabyrinthe.core.pickling.*

final class Map(using ComponentInit) extends ZonedSquareMap with EditableMap {
  type Square = com.funlabyrinthe.mazes.Square

  protected def squareIsPickleable: Pickleable[Square] = summon[Pickleable[Square]]

  override def getEditInterface(): MapEditInterface =
    new Map.EditInterface(this)
}

object Map {
  private class EditInterface(val map: Map) extends MapEditInterface {
    import map._

    def floors: Int = dimensions.z

    def getFloorRect(floor: Int): Rectangle2D =
      new Rectangle2D(0, 0, (dimensions.x+2)*SquareWidth,
          (dimensions.y+2)*SquareWidth)

    def drawFloor(context: DrawContext, floor: Int): Unit = {
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
      }
    }

    def getDescriptionAt(x: Double, y: Double, floor: Int): String =
      getPosAt(x, y, floor) map (p => map(p).toString()) getOrElse ""

    override def onMouseClicked(event: MouseEvent, floor: Int,
        component: Component): Unit = {
      getPosAt(event.x, event.y, floor) match {
        case Some(pos) =>
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
  }
}
