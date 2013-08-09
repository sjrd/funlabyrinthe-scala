package com.funlabyrinthe.mazes
package std

trait Hole extends Field {
  painter += "Fields/Hole"

  var message: String = "Aren't you crazy for wanting to jump in that hole!?"

  override def entering(context: MoveContext) = {
    import context._

    cancel()
    player.showMessage(message)
  }
}
