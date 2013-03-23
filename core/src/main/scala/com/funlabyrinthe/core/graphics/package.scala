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
}
