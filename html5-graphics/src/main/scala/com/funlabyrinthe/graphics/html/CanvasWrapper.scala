package com.funlabyrinthe.graphics.html

import com.funlabyrinthe.core.graphics._

import org.scalajs.dom

class CanvasWrapper(val delegate: dom.OffscreenCanvas) extends Canvas {
  def isComplete: Boolean = true

  def width: Double = delegate.width.toDouble
  def width_=(value: Double): Unit = delegate.width = value.toInt

  def height: Double = delegate.height.toDouble
  def height_=(value: Double): Unit = delegate.height = value.toInt

  private lazy val graphicsContext2D = {
    new GraphicsContextWrapper(
        delegate.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D])
  }

  def getGraphicsContext2D(): GraphicsContext = graphicsContext2D
}
