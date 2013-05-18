package com.funlabyrinthe
package mazes

import core._
import std._

class Mazes(implicit val universe: MazeUniverse) {

  object NoEffect extends Effect
  object NoTool extends Tool
  object NoObstacle extends Obstacle

  object Grass extends Grass
  object Water extends Water
  object Wall extends Wall
  object Hole extends Hole

  object NorthArrow extends Arrow {
    direction = North
    painter += "Arrows/NorthArrow"
  }
  object EastArrow extends Arrow {
    direction = East
    painter += "Arrows/EastArrow"
  }
  object SouthArrow extends Arrow {
    direction = North
    painter += "Arrows/SouthArrow"
  }
  object WestArrow extends Arrow {
    direction = East
    painter += "Arrows/WestArrow"
  }

  def initialize() {
    NoEffect
    NoTool
    NoObstacle

    Grass
    Water
    Wall
    Hole

    NorthArrow
    EastArrow
    SouthArrow
    WestArrow
  }
}
