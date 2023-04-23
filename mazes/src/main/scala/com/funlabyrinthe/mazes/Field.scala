package com.funlabyrinthe
package mazes

import core._

class Field()(implicit universe: Universe, originalID: ComponentID) extends VisualComponent {
  def this(id: ComponentID)(implicit universe: Universe) =
    this()(universe, id)

  category = ComponentCategory("fields", "Fields")

  def entering(context: MoveContext): Control[Unit] = doNothing()
  def exiting(context: MoveContext): Control[Unit] = doNothing()

  def entered(context: MoveContext): Control[Unit] = doNothing()
  def exited(context: MoveContext): Control[Unit] = doNothing()
}
