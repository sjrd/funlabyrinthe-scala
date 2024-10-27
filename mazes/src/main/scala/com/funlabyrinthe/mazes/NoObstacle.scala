package com.funlabyrinthe.mazes

import com.funlabyrinthe.core.*
import com.funlabyrinthe.core.graphics.*

final class NoObstacle private[mazes] (using ComponentInit) extends Obstacle:
  override def drawIcon(context: DrawContext): Unit =
    universe.DefaultIconPainter.drawTo(context)

  override def pushing(context: MoveContext): Unit = ()

  override protected def editMapRemove(pos: SquareRef): EditUserActionResult =
    EditUserActionResult.Unchanged
end NoObstacle
