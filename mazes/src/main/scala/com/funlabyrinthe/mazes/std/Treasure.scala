package com.funlabyrinthe.mazes
package std

trait Treasure extends Effect {
  override def execute(context: MoveContext) = {
    import context._
    player.win()
    // TODO Show message
  }
}
