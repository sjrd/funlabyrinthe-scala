package com.funlabyrinthe.core

import com.funlabyrinthe.core.pickling.Pickleable

final case class Dimensions(x: Int, y: Int, z: Int) derives Pickleable {
  def contains(pos: Position): Boolean = {
    val Position(px, py, pz) = pos
    px >= 0 && px < x && py >= 0 && py < y && pz >= 0 && pz < z
  }

  def withX(x: Int): Dimensions = Dimensions(x, this.y, this.z)

  def withY(y: Int): Dimensions = Dimensions(this.x, y, this.z)

  def withZ(z: Int): Dimensions = Dimensions(this.x, this.y, z)

  def toPosition: Position = Position(x, y, z)
}
