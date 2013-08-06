package com.funlabyrinthe
package mazes

import core._

class Field(override implicit val universe: MazeUniverse) extends VisualComponent {
  category = ComponentCategory("fields", "Fields")

  def entering(context: MoveContext) {}
  def exiting(context: MoveContext) {}

  def entered(context: MoveContext) {}
  def exited(context: MoveContext) {}
}
