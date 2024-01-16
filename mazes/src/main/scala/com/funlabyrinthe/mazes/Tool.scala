package com.funlabyrinthe.mazes

import com.funlabyrinthe.core.*

abstract class Tool(using ComponentInit) extends SquareComponent {
  category = ComponentCategory("tools", "Tools")

  def find(context: MoveContext): Control[Unit] = doNothing()
}
