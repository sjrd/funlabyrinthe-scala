package com.funlabyrinthe.coreinterface

import scala.scalajs.js

import org.scalajs.dom

trait Player extends js.Object:
  import Player.*

  def viewWidth: Double
  def viewHeight: Double

  def drawView(canvas: dom.HTMLCanvasElement): Unit

  def keyDown(code: KeyCode, shiftDown: Boolean, controlDown: Boolean,
      altDown: Boolean, metaDown: Boolean): Unit
end Player

object Player:
  opaque type KeyCode = Int

  object KeyCode:
    val Enter: KeyCode = 1
    val Left: KeyCode = 2
    val Up: KeyCode = 3
    val Right: KeyCode = 4
    val Down: KeyCode = 5
    val Other: KeyCode = 6
  end KeyCode
end Player
