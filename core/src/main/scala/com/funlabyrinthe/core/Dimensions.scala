package com.funlabyrinthe.core

final case class Dimensions(x: Int, y: Int, z: Int) {
  def contains(pos: Position): Boolean = {
    val Position(px, py, pz) = pos
    px >= 0 && px < x && py >= 0 && py < y && pz >= 0 && pz < z
  }
}
