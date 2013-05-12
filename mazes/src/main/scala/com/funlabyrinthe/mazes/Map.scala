package com.funlabyrinthe.mazes

import com.funlabyrinthe.core._
import graphics._

import javafx.scene.input.MouseEvent

class Map(_dimensions: Dimensions, _fill: Square)(
    override implicit val universe: MazeUniverse)
extends ZonedSquareMap with EditableMap {

  type Square = com.funlabyrinthe.mazes.Square

  resize(_dimensions, _fill)

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

    def drawFloor(context: DrawContext, floor: Int) {
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

    def getDescriptionAt(x: Double, y: Double): String = ""

    override def onMouseClicked(event: MouseEvent, component: Component) {}
  }
}
