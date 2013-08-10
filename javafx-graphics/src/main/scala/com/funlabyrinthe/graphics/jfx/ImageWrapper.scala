package com.funlabyrinthe.graphics.jfx

import com.funlabyrinthe.core.graphics._

class ImageWrapper(val delegate: javafx.scene.image.Image) extends Image {
  def width: Double = delegate.getWidth()
  def height: Double = delegate.getHeight()
}
