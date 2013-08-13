package com.funlabyrinthe.graphics.html

import com.funlabyrinthe.core.graphics._

import Conversions._

object HTML5GraphicsSystem extends GraphicsSystem {

  private def createCanvasElement(): jsdefs.HTMLCanvasElement = {
    jsdefs.Window.document.createElement(
        "canvas").asInstanceOf[jsdefs.HTMLCanvasElement]
  }

  def createCanvas(width: Double, height: Double): Canvas = {
    val element = createCanvasElement()
    element.width = width
    element.height = height
    new CanvasWrapper(element)
  }

  private lazy val measurer: jsdefs.CanvasRenderingContext2D = {
    val canvas = createCanvasElement()
    canvas.getContext("2d").asInstanceOf[jsdefs.CanvasRenderingContext2D]
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
