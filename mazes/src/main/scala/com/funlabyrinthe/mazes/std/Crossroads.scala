package com.funlabyrinthe.mazes
package std

trait Crossroads extends Effect {
  override def execute(context: MoveContext) = {
    import context._
    goOnMoving = true
  }
}
