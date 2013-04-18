package com.funlabyrinthe.core

import graphics._
import javafx.scene.input.MouseEvent

trait MapEditInterface {
  def floors: Int

  def getFloorRect(floor: Int): Rectangle2D
  def drawFloor(context: DrawContext, floor: Int): Unit

  def getDescriptionAt(x: Double, y: Double): String

  def onMouseClicked(event: MouseEvent, selectedComponent: Component) {}
}
