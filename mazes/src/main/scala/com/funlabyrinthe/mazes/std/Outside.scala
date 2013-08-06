package com.funlabyrinthe.mazes
package std

trait Outside extends Field {
  painter += "Fields/Outside"

  override def entering(context: MoveContext) {
    import context._

    player.win()
    // TODO Show message
  }
}
