package com.funlabyrinthe.core.graphics

trait GraphicsSystem {
  def createCanvas(width: Int, height: Int): Canvas
  def createFrameCanvas(width: Int, height: Int, time: Int): Canvas

  def createAnimated(frames: List[Canvas]): Image

  def measureText(text: String, font: Font): (Double, Double)
}
