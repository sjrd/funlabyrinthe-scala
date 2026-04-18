package com.funlabyrinthe.core

import com.funlabyrinthe.core.scene.*
import graphics._

trait ResourceLoader {
  def loadImage(name: String): Option[Image]

  def loadGraphic(name: String, width: Int, height: Int): Graphic
}
