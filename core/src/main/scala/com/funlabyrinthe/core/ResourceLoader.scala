package com.funlabyrinthe.core

import graphics._

trait ResourceLoader {
  def loadImage(name: String): Option[Image]
}
