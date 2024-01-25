package com.funlabyrinthe.coreinterface

import scala.scalajs.js

import org.scalajs.dom

trait EditableMap extends js.Object:
  import EditableMap.*

  def fullID: String
  def shortID: String

  def floors: Int

  def getFloorRect(floor: Int): Dimensions2D

  def drawFloor(floor: Int): dom.ImageBitmap

  def getDescriptionAt(x: Double, y: Double, floor: Int): String

  def onMouseClicked(x: Double, y: Double, floor: Int, selectedComponent: EditableComponent): EditUserActionResult

  def newResizingView(): ResizingView
end EditableMap

object EditableMap:
  type ResizingDirection = "north" | "east" | "south" | "west" | "up" | "down"

  trait ResizingView extends EditableMap:
    def canResize(direction: ResizingDirection, grow: Boolean): Boolean

    def resize(direction: ResizingDirection, grow: Boolean): Unit

    def commit(): Unit
  end ResizingView
end EditableMap
