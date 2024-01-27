package com.funlabyrinthe.mazes

import cps.customValueDiscard

import com.funlabyrinthe.core.*

abstract class Obstacle(using ComponentInit) extends SquareComponent {
  category = ComponentCategory("obstacles", "Obstacles")

  def pushing(context: MoveContext): Control[Unit] = control {
    context.cancel()
  }

  protected def editMapAdd(pos: SquareRef): EditUserActionResult =
    pos() += this
    EditUserActionResult.Done
  end editMapAdd

  protected def editMapRemove(pos: SquareRef): EditUserActionResult =
    pos() += noObstacle
    EditUserActionResult.Done
  end editMapRemove
}
