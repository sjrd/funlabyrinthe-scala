package com.funlabyrinthe
package mazes

import core._

class Effect(implicit override val universe: MazeUniverse,
    originalID: ComponentID) extends VisualComponent {

  def this(id: ComponentID)(implicit universe: MazeUniverse) =
    this()(universe, id)

  category = ComponentCategory("effects", "Effects")

  def entered(context: MoveContext): Unit @control = ()
  def exited(context: MoveContext): Unit @control = ()

  def execute(context: MoveContext): Unit @control = ()
}
