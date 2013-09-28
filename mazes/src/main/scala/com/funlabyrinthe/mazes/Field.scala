package com.funlabyrinthe
package mazes

import core._

class Field()(implicit override val universe: MazeUniverse,
    originalID: ComponentID) extends VisualComponent {

  def this(id: ComponentID)(implicit universe: MazeUniverse) =
    this()(universe, id)

  category = ComponentCategory("fields", "Fields")

  def entering(context: MoveContext): Unit @control = ()
  def exiting(context: MoveContext): Unit @control = ()

  def entered(context: MoveContext): Unit @control = ()
  def exited(context: MoveContext): Unit @control = ()
}
