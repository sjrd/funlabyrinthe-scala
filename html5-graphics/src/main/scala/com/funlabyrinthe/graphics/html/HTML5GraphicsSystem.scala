package com.funlabyrinthe.graphics.html

import org.scalajs.dom

import com.funlabyrinthe.core.graphics._

import Conversions._

object HTML5GraphicsSystem extends GraphicsSystem {

  private def createCanvasElement(): dom.HTMLCanvasElement = {
    dom.window.document.createElement(
        "canvas").asInstanceOf[dom.HTMLCanvasElement]
  }

  def createCanvas(width: Double, height: Double): Canvas = {
    val element = createCanvasElement()
    element.width = width
    element.height = height
    new CanvasWrapper(element)
  }

  private lazy val measurer: dom.CanvasRenderingContext2D = {
    val canvas = createCanvasElement()
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
