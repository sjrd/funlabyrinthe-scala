package com.funlabyrinthe.graphics.html

import com.funlabyrinthe.core.graphics._

import org.scalajs.dom

class CanvasWrapper(val delegate: dom.OffscreenCanvas, val time: Int) extends Canvas {
  def isComplete: Boolean = true

  def width: Int = delegate.width.toInt // it is actually an integer in the spec
  def width_=(value: Int): Unit = delegate.width = value

  def height: Int = delegate.height.toInt // it is actually an integer in the spec
  def height_=(value: Int): Unit = delegate.height = value

  def isAnimated: Boolean = false
  def frames: IArray[Image] = Constants.EmptyImageArray

  private lazy val graphicsContext2D = {
    new GraphicsContextWrapper(
        delegate.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D])
  }

  def getGraphicsContext2D(): GraphicsContext = graphicsContext2D
}
