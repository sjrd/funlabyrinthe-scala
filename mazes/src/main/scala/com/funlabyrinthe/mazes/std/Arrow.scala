package com.funlabyrinthe.mazes.std

import cps.customValueDiscard

import com.funlabyrinthe.core.*
import com.funlabyrinthe.core.graphics.Painter
import com.funlabyrinthe.mazes.*

class Arrow(using ComponentInit) extends Effect derives Reflector {
  var direction: Direction = Direction.North // we need a default

  override def reflect() = autoReflect[Arrow]

  override def execute(context: MoveContext) = control {
    import context._
    player.direction = Some(direction)
    goOnMoving = true
  }
}

object Arrow:
  def make(name: String, direction: Direction, painterItem: Painter.PainterItem)(using ComponentInit): Arrow =
    val arrow = new Arrow
    arrow.name = name
    arrow.direction = direction
    arrow.painter += painterItem
    arrow
  end make
end Arrow
