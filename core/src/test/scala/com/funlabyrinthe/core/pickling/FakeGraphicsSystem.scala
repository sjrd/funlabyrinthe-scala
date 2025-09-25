package com.funlabyrinthe.core.pickling

import com.funlabyrinthe.core.graphics.*

class FakeGraphicsSystem extends GraphicsSystem:
  def createCanvas(width: Int, height: Int): Canvas =
    throw UnsupportedOperationException("FakeGraphicsSystem.createCanvas")

  def createFrameCanvas(width: Int, height: Int, time: Int): Canvas =
    throw UnsupportedOperationException("FakeGraphicsSystem.createFrameCanvas")

  def createAnimated(frames: List[Canvas]): Image =
    throw UnsupportedOperationException("FakeGraphicsSystem.createAnimated")

  def measureText(text: String, font: Font): (Double, Double) =
    throw UnsupportedOperationException("FakeGraphicsSystem.measureText")
end FakeGraphicsSystem
