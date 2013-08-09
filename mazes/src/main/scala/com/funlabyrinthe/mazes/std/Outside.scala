package com.funlabyrinthe.mazes
package std

trait Outside extends Field {
  painter += "Fields/Outside"

  var message: String = "Congratulations! You found the exit!"

  override def entering(context: MoveContext) = {
    import context._

    player.win()
    player.showMessage(message)
  }
}
