package com.funlabyrinthe.graphics.jfx

import com.funlabyrinthe.core.graphics._

class CanvasWrapper(val delegate: javafx.scene.canvas.Canvas) extends Canvas {
  def width: Double = delegate.getWidth()
  def width_=(value: Double): Unit = delegate.setWidth(value)

  def height: Double = delegate.getHeight()
  def height_=(value: Double): Unit = delegate.setHeight(value)

  private lazy val graphicsContext2D =
    new GraphicsContextWrapper(delegate.getGraphicsContext2D())

  def getGraphicsContext2D(): GraphicsContext = graphicsContext2D
}
