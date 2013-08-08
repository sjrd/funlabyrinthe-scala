package com.funlabyrinthe.mazes
package std

trait Water extends Field {
  painter += "Fields/Water"

  override def entering(context: MoveContext) = {
    import context._

    if (player cannot GoOnWater)
      cancel()
  }
}
