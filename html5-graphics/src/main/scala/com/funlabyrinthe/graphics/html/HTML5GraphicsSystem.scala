package com.funlabyrinthe.graphics.html

import org.scalajs.dom

import Conversions._

object HTML5GraphicsSystem {

  def createCanvas(width: Int, height: Int): CanvasWrapper =
    createFrameCanvas(width, height, time = 0)

  def createFrameCanvas(width: Int, height: Int, time: Int): CanvasWrapper =
    val canvas = new dom.OffscreenCanvas(width, height)
    new CanvasWrapper(canvas, time)

  def createAnimated(frames: List[CanvasWrapper]): Image =
    new Animated(IArray.from(frames))
}
