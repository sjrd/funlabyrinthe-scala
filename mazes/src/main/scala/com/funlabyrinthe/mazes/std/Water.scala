package com.funlabyrinthe.mazes
package std

trait Water extends Field {
  painter += "Fields/Water"

  override def entering(context: MoveContext) {
    // TODO Check if the player can go on water
    // TODO Display message
    context.cancel()
  }
}
