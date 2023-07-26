package com.funlabyrinthe.core.pickling

import com.funlabyrinthe.core.UniverseEnvironment
import com.funlabyrinthe.core.graphics.*

object FakeEnvironment:
  private class FakeGraphicsSystem extends GraphicsSystem:
    def createCanvas(width: Double, height: Double): Canvas =
      throw UnsupportedOperationException("FakeGraphicsSystem.createCanvas")

    def measureText(text: String, font: Font): (Double, Double) =
      throw UnsupportedOperationException("FakeGraphicsSystem.measureText")
  end FakeGraphicsSystem

  val Instance: UniverseEnvironment =
    new UniverseEnvironment(new FakeGraphicsSystem, new FakeResourceLoader)
end FakeEnvironment
