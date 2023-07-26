package com.funlabyrinthe.core

import com.funlabyrinthe.core.pickling.Pickleable

final case class Dimensions(x: Int, y: Int, z: Int) derives Pickleable {
  def contains(pos: Position): Boolean = {
    val Position(px, py, pz) = pos
    px >= 0 && px < x && py >= 0 && py < y && pz >= 0 && pz < z
  }
}
