package com.funlabyrinthe.mazes
package std

trait Hole extends Field {
  painter += "Fields/Hole"

  override def entering(context: MoveContext) {
    // TODO Display message
    context.cancel()
  }
}
