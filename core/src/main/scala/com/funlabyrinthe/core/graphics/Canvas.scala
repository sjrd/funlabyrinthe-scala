package com.funlabyrinthe.core.graphics

trait Canvas {
  def width: Double
  def width_=(value: Double): Unit

  def height: Double
  def height_=(value: Double): Unit

  def getGraphicsContext2D(): GraphicsContext
}
