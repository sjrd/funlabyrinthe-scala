package com.funlabyrinthe.graphics.html

import com.funlabyrinthe.core.graphics._

import org.scalajs.dom

class CanvasWrapper(val delegate: dom.HTMLCanvasElement) extends Canvas {
  def width: Double = delegate.width
  def width_=(value: Double): Unit = delegate.width = value

  def height: Double = delegate.height
  def height_=(value: Double): Unit = delegate.height = value

  private lazy val graphicsContext2D = {
    new GraphicsContextWrapper(
        delegate.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D])
  }

  def getGraphicsContext2D(): GraphicsContext = graphicsContext2D
}
