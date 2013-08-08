package com.funlabyrinthe.mazes
package std

trait Wall extends Field {
  painter += "Fields/Wall"

  override def entering(context: MoveContext) = {
    context.cancel()
  }
}
