package com.funlabyrinthe
package mazes

import core._

class Obstacle(override implicit val universe: MazeUniverse) extends VisualComponent {
  def pushing(context: MoveContext) {}
}
