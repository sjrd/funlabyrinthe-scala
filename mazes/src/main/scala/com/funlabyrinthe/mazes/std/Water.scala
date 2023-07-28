package com.funlabyrinthe.mazes
package std

import cps.customValueDiscard

import com.funlabyrinthe.core._

class Water(using ComponentInit) extends Field {
  painter += "Fields/Water"

  override def entering(context: MoveContext) = control {
    import context._

    if (exec(player cannot GoOnWater))
      cancel()
  }
}
