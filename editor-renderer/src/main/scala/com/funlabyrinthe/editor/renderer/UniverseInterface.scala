package com.funlabyrinthe.editor.renderer

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import com.funlabyrinthe.core.*
import com.funlabyrinthe.core.graphics.*
import com.funlabyrinthe.core.input.*
import com.funlabyrinthe.graphics.html.GraphicsContextWrapper

import org.scalajs.dom.CanvasRenderingContext2D
import org.scalajs.dom.ImageBitmap
import org.scalajs.dom.OffscreenCanvas

final class UniverseInterface(universe: Universe, mapID: String, currentFloor: Int, selectedComponentID: String):
  import UniverseInterface.*

  val map = Map.buildFromUniverse(universe, mapID, currentFloor)

  def mouseClickOnMap(event: MouseEvent): Future[UniverseInterface] =
    Future {
      val editInterface = universe.getComponentByID(mapID).asInstanceOf[EditableMap].getEditInterface()
      val selectedComponent = universe.getComponentByID(selectedComponentID)
      editInterface.onMouseClicked(event, currentFloor, selectedComponent)
      new UniverseInterface(universe, mapID, currentFloor, selectedComponentID)
    }
  end mouseClickOnMap
end UniverseInterface

object UniverseInterface:
  final class Map(
    val id: String,
    val floors: Int,
    val currentFloor: Int,
    val currentFloorRect: Rectangle2D,
    val floorImage: ImageBitmap
  )

  object Map:
    def buildFromUniverse(universe: Universe, mapID: String, currentFloor: Int): Map =
      val underlying = universe.getComponentByID(mapID).asInstanceOf[EditableMap].getEditInterface()
      val floors = underlying.floors
      val currentFloorRect = underlying.getFloorRect(currentFloor)
      val floorImage = drawFloor(underlying, currentFloor, currentFloorRect)
      Map(mapID, floors, currentFloor, currentFloorRect, floorImage)
    end buildFromUniverse

    private def drawFloor(mapInterface: MapEditInterface, floor: Int, floorRect: Rectangle2D): ImageBitmap =
      val canvas = new OffscreenCanvas(floorRect.width, floorRect.height)
      val gc = canvas.getContext("2d").asInstanceOf[CanvasRenderingContext2D]
      val context = new DrawContext(new GraphicsContextWrapper(gc), floorRect)
      mapInterface.drawFloor(context, floor)
      canvas.transferToImageBitmap()
    end drawFloor
  end Map
end UniverseInterface
