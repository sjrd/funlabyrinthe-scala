package com.funlabyrinthe
package mazes

import core._

trait MazeUniverse extends Universe {
  val mazes = new Mazes

  override def initialize(): Unit = {
    super.initialize()
    mazes.initialize()
  }
}

object MazeUniverse:
  extension (universe: Universe)
    def asMazeUniverse: MazeUniverse = universe.asInstanceOf[MazeUniverse]
    def mazes: Mazes = asMazeUniverse.mazes
end MazeUniverse
