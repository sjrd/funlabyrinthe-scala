package com.funlabyrinthe.core

package object graphics {
  // Alias a bunch of things from ScalaFX and JavaFX that we need all the time

  type Color = scalafx.scene.paint.Color
  val Color = scalafx.scene.paint.Color

  type Font = scalafx.scene.text.Font
  val Font = scalafx.scene.text.Font

  type Point2D = scalafx.geometry.Point2D

  type Rectangle2D = scalafx.geometry.Rectangle2D
  val Rectangle2D = scalafx.geometry.Rectangle2D

  type Image = scalafx.scene.image.Image

  type Canvas = scalafx.scene.canvas.Canvas

  type GraphicsContext = scalafx.scene.canvas.GraphicsContext

  // Functions

  def fillWithOpaqueBackground(context: DrawContext) {
    import context._
    val SmallSquareSize = 16.0

    gc.save

    val evenFill = Color.BLACK
    val oddFill = Color.DIMGRAY

    val xrange = rect.minX until rect.maxX by SmallSquareSize
    val yrange = rect.minY until rect.maxY by SmallSquareSize

    for (i <- 0 until xrange.length; j <- 0 until yrange.length) {
      gc.fill = if ((i+j) % 2 == 0) evenFill else oddFill
      gc.fillRect(xrange(i), yrange(j), SmallSquareSize, SmallSquareSize)
    }

    gc.restore
  }
}
