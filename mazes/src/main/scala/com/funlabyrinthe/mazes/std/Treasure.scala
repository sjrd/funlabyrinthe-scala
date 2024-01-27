package com.funlabyrinthe.mazes.std

import com.funlabyrinthe.core.*
import com.funlabyrinthe.mazes.*

class Treasure(using ComponentInit) extends Effect {
  painter += "Chests/Treasure"
  var message: String = "Congratulations! You found the treasure!"

  override def execute(context: MoveContext) = {
    import context._

    player.win()
    player.showMessage(message)
  }
}
