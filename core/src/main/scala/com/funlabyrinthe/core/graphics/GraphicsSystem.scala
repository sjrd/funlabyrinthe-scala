package com.funlabyrinthe.core.graphics

trait GraphicsSystem {
  def createCanvas(width: Double, height: Double): Canvas

  def loadImage(url: String): Image

  def measureText(text: String, font: Font): (Double, Double)
}
