package com.funlabyrinthe.core

import graphics._
import input._

trait MapEditInterface {
  def floors: Int

  def getFloorRect(floor: Int): Rectangle2D
  def drawFloor(context: DrawContext, floor: Int): Unit

  def getDescriptionAt(x: Double, y: Double, floor: Int): String

  def onMouseClicked(event: MouseEvent, floor: Int,
      selectedComponent: Component) {}
}
