package com.funlabyrinthe.mazes.std

import cps.customValueDiscard

import com.funlabyrinthe.core.*
import com.funlabyrinthe.core.graphics.Painter

import com.funlabyrinthe.mazes.*

class SecretWay(using ComponentInit) extends Obstacle:
  painter += "Fields/Wall"
  editVisualTag = "!"

  override def pushing(context: MoveContext): Control[Unit] = control {
    context.cancel()

    if context.keyEvent.isDefined then
      context.pos() += noObstacle
  }
end SecretWay
