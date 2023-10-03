package com.funlabyrinthe.mazes

import com.funlabyrinthe.core.*

final class NoObstacle private[mazes] (using ComponentInit) extends Obstacle:
  import universe.*

  name = "(no obstacle)"

  override def drawIcon(context: DrawContext): Unit =
    DefaultIconPainter.drawTo(context)
end NoObstacle
