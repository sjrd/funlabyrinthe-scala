package com.funlabyrinthe.mazes
package std

import com.funlabyrinthe.core.*

class Outside(using ComponentInit) extends Field {
  painter += "Fields/Outside"

  var message: String = "Congratulations! You found the exit!"

  override def entered(context: MoveContext) = {
    import context._

    player.win()
    player.showMessage(message)
  }
}
