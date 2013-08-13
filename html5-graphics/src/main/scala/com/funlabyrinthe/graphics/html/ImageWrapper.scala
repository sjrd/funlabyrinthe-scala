package com.funlabyrinthe.graphics.html

import com.funlabyrinthe.core.graphics._

class ImageWrapper(val delegate: jsdefs.Image) extends Image {
  def width: Double = delegate.width
  def height: Double = delegate.height
}
