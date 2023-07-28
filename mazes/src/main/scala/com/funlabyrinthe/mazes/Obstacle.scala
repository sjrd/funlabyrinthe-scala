package com.funlabyrinthe
package mazes

import core._

abstract class Obstacle(using ComponentInit) extends VisualComponent {
  category = ComponentCategory("obstacles", "Obstacles")

  def pushing(context: MoveContext): Control[Unit] = doNothing()
}
