package com.funlabyrinthe.corebridge

import scala.scalajs.js

import org.scalajs.dom

import com.funlabyrinthe.core
import com.funlabyrinthe.coreinterface as intf

import com.funlabyrinthe.graphics.html.GraphicsContextWrapper

final class EditableMap(universe: Universe, underlying: core.EditableMap) extends intf.EditableMap:
  private val editInterface = underlying.getEditInterface()

  def id: String = underlying.id

  def floors: Int = editInterface.floors

  def getFloorRect(floor: Int): intf.Dimensions2D =
    val coreRect = editInterface.getFloorRect(floor)
    intf.Dimensions2D(coreRect.width, coreRect.height)

  def drawFloor(floor: Int): dom.ImageBitmap =
    val coreRect = editInterface.getFloorRect(floor)
    val canvas = new dom.OffscreenCanvas(coreRect.width, coreRect.height)
    val gc = canvas.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]
    val drawContext = new core.graphics.DrawContext(new GraphicsContextWrapper(gc), coreRect)
    editInterface.drawFloor(drawContext, floor)
    canvas.transferToImageBitmap()
  end drawFloor

  def getDescriptionAt(x: Double, y: Double, floor: Int): String =
    editInterface.getDescriptionAt(x, y, floor)

  def onMouseClicked(x: Double, y: Double, floor: Int, selectedComponent: intf.EditableComponent): Unit =
    val event = new core.input.MouseEvent(x, y, core.input.MouseButton.Primary)
    val component = selectedComponent.asInstanceOf[EditableComponent]
    editInterface.onMouseClicked(event, floor, component.underlying)
  end onMouseClicked
end EditableMap
