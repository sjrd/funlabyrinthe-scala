package com.funlabyrinthe.mazes.std

import scala.Conversion.into

import com.funlabyrinthe.core.*
import com.funlabyrinthe.core.scene.Painter
import com.funlabyrinthe.mazes.*

class Arrow(using ComponentInit) extends Effect {
  var direction: Direction = Direction.North // we need a default

  override def execute(context: ExecuteContext): Unit = {
    context.player.direction = direction
    context.goOnMoving = true
  }
}

object Arrow:
  def make(direction: Direction, painterItem: into[Painter.PainterItem])(using ComponentInit): Arrow =
    val arrow = new Arrow
    arrow.direction = direction
    arrow.painter += painterItem
    arrow
  end make
end Arrow
