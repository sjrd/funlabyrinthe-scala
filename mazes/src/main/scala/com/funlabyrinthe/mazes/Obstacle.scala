package com.funlabyrinthe
package mazes

import core._

class Obstacle(implicit universe: Universe, originalID: ComponentID) extends VisualComponent {
  def this(id: ComponentID)(implicit universe: Universe) =
    this()(universe, id)

  category = ComponentCategory("obstacles", "Obstacles")

  def pushing(context: MoveContext): Control[Unit] = doNothing()
}
