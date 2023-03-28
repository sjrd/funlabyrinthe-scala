package com.funlabyrinthe.mazes
package std

import com.funlabyrinthe.core._

import cps.customValueDiscard

trait Arrow extends Effect {
  var direction: Direction = North // we need a default

  override def execute(context: MoveContext) = control {
    import context._
    player.direction = Some(direction)
    goOnMoving = true
  }
}
