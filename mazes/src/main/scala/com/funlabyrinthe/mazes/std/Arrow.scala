package com.funlabyrinthe.mazes
package std

import com.funlabyrinthe.core._

import cps.customValueDiscard

class Arrow(using ComponentInit) extends Effect derives Reflector {
  var direction: Direction = North // we need a default

  override def reflect() = autoReflect[Arrow]

  override def execute(context: MoveContext) = control {
    import context._
    player.direction = Some(direction)
    goOnMoving = true
  }
}
