package com.funlabyrinthe.mazes

import cps.customValueDiscard

import com.funlabyrinthe.core.*

abstract class Obstacle(using ComponentInit) extends SquareComponent {
  category = ComponentCategory("obstacles", "Obstacles")

  def pushing(context: MoveContext): Control[Unit] = control {
    context.cancel()
  }
}
