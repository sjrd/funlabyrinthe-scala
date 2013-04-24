package com.funlabyrinthe
package mazes

import core._

class Mazes(implicit val universe: MazeUniverse) {

  object NoEffect extends Effect
  object NoTool extends Tool
  object NoObstacle extends Obstacle

  object Grass extends Ground {
    painter += "Fields/Grass"
  }

  object Water extends Ground {
    painter += "Fields/Water"
  }

  object Wall extends Ground {
    painter += "Fields/Wall"
  }

  object Hole extends Ground {
    painter += "Fields/Hole"
  }

  def initialize() {
    NoEffect
    NoTool
    NoObstacle

    Grass
    Water
    Wall
    Hole
  }
}
