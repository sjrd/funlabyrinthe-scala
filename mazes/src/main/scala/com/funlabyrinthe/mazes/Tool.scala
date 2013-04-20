package com.funlabyrinthe
package mazes

import core._

class Tool(override implicit val universe: MazeUniverse) extends VisualComponent {
  def find(context: MoveContext) {}
}
