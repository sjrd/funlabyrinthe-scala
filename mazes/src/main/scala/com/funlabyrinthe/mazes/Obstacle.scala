package com.funlabyrinthe
package mazes

import core._

class Obstacle(override implicit val universe: MazeUniverse) extends VisualComponent {
  category = ComponentCategory("obstacles", "Obstacles")

  def pushing(context: MoveContext) {}
}
