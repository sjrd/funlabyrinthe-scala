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
}
