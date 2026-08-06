package com.funlabyrinthe.core

import com.funlabyrinthe.core.graphics._
import com.funlabyrinthe.core.input._
import com.funlabyrinthe.core.scene.SceneUpdateFragment

trait MapEditInterface:
  import MapEditInterface.*

  def floors: Int

  def getFloorRect(floor: Int): Rectangle2D
  def drawFloor(context: DrawContext, floor: Int): Unit
  def presentFloor(floor: Int): SceneUpdateFragment

  def getDescriptionAt(x: Double, y: Double, floor: Int): String

  def onMouseClicked(event: MouseEvent, floor: Int, selectedComponent: Component)(
      using EditingServices): Unit

  def newResizingView(): ResizingView
end MapEditInterface

object MapEditInterface:
  trait ResizingView extends MapEditInterface:
    def canResize(direction: Direction3D, grow: Boolean): Boolean

    def resize(direction: Direction3D, grow: Boolean): Unit

    def commit(): Unit
  end ResizingView
end MapEditInterface
