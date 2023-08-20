package com.funlabyrinthe.coreinterface

import scala.scalajs.js

import org.scalajs.dom

trait EditableMap extends js.Object:
  def id: String
  def floors: Int

  def getFloorRect(floor: Int): Dimensions2D

  def drawFloor(floor: Int): dom.ImageBitmap

  def getDescriptionAt(x: Double, y: Double, floor: Int): String

  def onMouseClicked(x: Double, y: Double, floor: Int, selectedComponent: EditableComponent): Unit
end EditableMap
