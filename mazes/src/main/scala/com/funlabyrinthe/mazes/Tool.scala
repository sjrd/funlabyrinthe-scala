package com.funlabyrinthe
package mazes

import core._

class Tool(override implicit val universe: MazeUniverse) extends VisualComponent {
  category = ComponentCategory("tools", "Tools")

  def find(context: MoveContext) {}
}
