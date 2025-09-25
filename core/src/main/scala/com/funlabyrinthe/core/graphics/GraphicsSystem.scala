package com.funlabyrinthe.core.graphics

trait GraphicsSystem {
  def createCanvas(width: Double, height: Double): Canvas
  def createFrameCanvas(width: Double, height: Double, time: Int): Canvas

  def createAnimated(frames: List[Canvas]): Image

  def measureText(text: String, font: Font): (Double, Double)
}
