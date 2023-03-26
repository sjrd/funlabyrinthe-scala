package com.funlabyrinthe.graphics.html

import org.scalajs.dom

import com.funlabyrinthe.core.graphics._

class ImageWrapper(val delegate: dom.HTMLImageElement) extends Image {
  def width: Double = delegate.width
  def height: Double = delegate.height
}
