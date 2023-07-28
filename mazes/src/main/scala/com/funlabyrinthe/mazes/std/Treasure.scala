package com.funlabyrinthe.mazes
package std

import com.funlabyrinthe.core.*

class Treasure(using ComponentInit) extends Effect {
  var message: String = "Congratulations! You found the treasure!"

  override def execute(context: MoveContext) = {
    import context._

    player.win()
    player.showMessage(message)
  }
}
