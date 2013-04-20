package com.funlabyrinthe
package mazes

import core._

class Effect(override implicit val universe: MazeUniverse) extends VisualComponent {
  def entered(context: MoveContext) {}
  def exited(context: MoveContext) {}

  def execute(context: MoveContext) {}
}
