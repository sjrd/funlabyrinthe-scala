package com.funlabyrinthe.core.pickling

import com.funlabyrinthe.core.graphics.*

class FakeGraphicsSystem extends GraphicsSystem:
  def createCanvas(width: Double, height: Double): Canvas = ???

  def measureText(text: String, font: Font): (Double, Double) = ???
end FakeGraphicsSystem
