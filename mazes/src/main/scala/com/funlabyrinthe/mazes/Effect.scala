package com.funlabyrinthe
package mazes

import core._

class Effect(override implicit val universe: MazeUniverse) extends VisualComponent {
  category = ComponentCategory("effects", "Effects")

  def entered(context: MoveContext): Unit @control = ()
  def exited(context: MoveContext): Unit @control = ()

  def execute(context: MoveContext): Unit @control = ()
}
