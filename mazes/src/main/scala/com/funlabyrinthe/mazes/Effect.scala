package com.funlabyrinthe
package mazes

import core._

class Effect(implicit universe: Universe, originalID: ComponentID) extends VisualComponent {
  def this(id: ComponentID)(implicit universe: Universe) =
    this()(universe, id)

  category = ComponentCategory("effects", "Effects")

  def entered(context: MoveContext): Control[Unit] = doNothing()
  def exited(context: MoveContext): Control[Unit] = doNothing()

  def execute(context: MoveContext): Control[Unit] = doNothing()
}
