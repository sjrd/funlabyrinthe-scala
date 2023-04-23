package com.funlabyrinthe
package mazes

import core._

class Tool(implicit universe: Universe, originalID: ComponentID) extends VisualComponent {
  def this(id: ComponentID)(implicit universe: Universe) =
    this()(universe, id)

  category = ComponentCategory("tools", "Tools")

  def find(context: MoveContext): Control[Unit] = doNothing()
}
