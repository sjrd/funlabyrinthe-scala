package com.funlabyrinthe.mazes
package std

trait Sky extends Field {
  painter += "Fields/Sky"

  override def entering(context: MoveContext) {
    context.cancel()
    // TODO Show message? Maybe once.
  }
}
