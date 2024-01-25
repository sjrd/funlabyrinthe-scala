package com.funlabyrinthe.mazes

import com.funlabyrinthe.core.*

final class NoObstacle private[mazes] (using ComponentInit) extends Obstacle:
  import universe.*

  name = "(no obstacle)"

  override def drawIcon(context: DrawContext): Unit =
    DefaultIconPainter.drawTo(context)

  override def pushing(context: MoveContext): Control[Unit] = doNothing()

  override protected def editMapRemove(pos: SquareRef[Map]): EditUserActionResult =
    EditUserActionResult.Unchanged
end NoObstacle
