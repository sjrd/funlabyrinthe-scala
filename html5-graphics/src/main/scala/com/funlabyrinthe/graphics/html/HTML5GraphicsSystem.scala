package com.funlabyrinthe.graphics.html

import org.scalajs.dom

import com.funlabyrinthe.core.graphics._

import Conversions._

object HTML5GraphicsSystem extends GraphicsSystem {

  def createCanvas(width: Double, height: Double): Canvas =
    createFrameCanvas(width, height, time = 0)

  def createFrameCanvas(width: Double, height: Double, time: Int): Canvas =
    val canvas = new dom.OffscreenCanvas(width, height)
    new CanvasWrapper(canvas, time)

  def createAnimated(frames: List[Canvas]): Image =
    new Animated(IArray.from(frames))

  private lazy val measurer: dom.CanvasRenderingContext2D = {
    val canvas = new dom.OffscreenCanvas(1.0, 1.0)
    canvas.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]
  }

  def measureText(text: String, font: Font): (Double, Double) = {
    measurer.save()
    measurer.font = coreFont2html(font)
    val width: Double = measurer.measureText(text).width
    measurer.rotate(90)
    val height: Double = measurer.measureText("M").width
    measurer.restore()
    (width, height)
  }
}
