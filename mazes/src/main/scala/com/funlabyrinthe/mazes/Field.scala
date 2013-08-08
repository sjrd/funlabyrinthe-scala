package com.funlabyrinthe
package mazes

import core._

class Field(override implicit val universe: MazeUniverse) extends VisualComponent {
  category = ComponentCategory("fields", "Fields")

  def entering(context: MoveContext): Unit @control = ()
  def exiting(context: MoveContext): Unit @control = ()

  def entered(context: MoveContext): Unit @control = ()
  def exited(context: MoveContext): Unit @control = ()
}
