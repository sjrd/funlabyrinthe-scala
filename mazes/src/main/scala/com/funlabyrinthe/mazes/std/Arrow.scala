package com.funlabyrinthe.mazes.std

import scala.Conversion.into

import com.funlabyrinthe.core.*
import com.funlabyrinthe.core.graphics.Painter
import com.funlabyrinthe.mazes.*

class Arrow(using ComponentInit) extends Effect {
  var direction: Direction = Direction.North // we need a default

  override def execute(context: MoveContext): Unit = {
    import context._
    player.direction = Some(direction)
    goOnMoving = true
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
