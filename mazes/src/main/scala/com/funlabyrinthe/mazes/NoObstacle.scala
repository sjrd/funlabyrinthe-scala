package com.funlabyrinthe.mazes

import com.funlabyrinthe.core.*

final class NoObstacle private[mazes] (using ComponentInit) extends Obstacle:
  import universe.*

  override def drawIcon(context: DrawContext): Unit =
    DefaultIconPainter.drawTo(context)

  override def pushing(context: MoveContext): Control[Unit] = doNothing()

  override protected def editMapRemove(pos: SquareRef): EditUserActionResult =
    EditUserActionResult.Unchanged
end NoObstacle
