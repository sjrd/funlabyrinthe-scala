package com.funlabyrinthe.core

import graphics._

import indigo.Graphic
import indigo.Material

trait ResourceLoader {
  def loadImage(name: String): Option[Image]

  def loadGraphic(name: String, width: Int, height: Int): Graphic[Material.ImageEffects]
}
