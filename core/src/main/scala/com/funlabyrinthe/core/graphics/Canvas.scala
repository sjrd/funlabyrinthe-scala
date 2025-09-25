package com.funlabyrinthe.core.graphics

trait Canvas extends Image {
  def width: Int
  def width_=(value: Int): Unit

  def height: Int
  def height_=(value: Int): Unit

  def getGraphicsContext2D(): GraphicsContext
}
