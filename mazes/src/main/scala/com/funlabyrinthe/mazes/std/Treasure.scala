package com.funlabyrinthe.mazes
package std

trait Treasure extends Effect {
  var message: String = "Congratulations! You found the treasure!"

  override def execute(context: MoveContext) = {
    import context._

    player.win()
    player.showMessage(message)
  }
}
