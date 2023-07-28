package com.funlabyrinthe
package mazes

import core._

abstract class Tool(using ComponentInit) extends VisualComponent {
  category = ComponentCategory("tools", "Tools")

  def find(context: MoveContext): Control[Unit] = doNothing()
}
