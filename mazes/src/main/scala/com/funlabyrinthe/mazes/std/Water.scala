package com.funlabyrinthe.mazes
package std

import cps.customValueDiscard

import com.funlabyrinthe.core._

trait Water extends Field {
  painter += "Fields/Water"

  override def entering(context: MoveContext) = control {
    import context._

    if (exec(player cannot GoOnWater))
      cancel()
  }
}
