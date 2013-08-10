package com.funlabyrinthe.graphics.jfx

import com.funlabyrinthe.core.graphics._

import Conversions._

object JavaFXGraphicsSystem extends GraphicsSystem {

  def createCanvas(width: Double, height: Double): Canvas =
    new CanvasWrapper(new javafx.scene.canvas.Canvas(width, height))

  def measureText(text: String, font: Font): (Double, Double) = {
    val textControl = new javafx.scene.text.Text(text)
    textControl.setFont(font)
    textControl.snapshot(null, null)
    val bounds = textControl.getLayoutBounds
    (bounds.getWidth, bounds.getHeight)
  }

}
