package com.funlabyrinthe.mazes
package std

import cps.customValueDiscard

import com.funlabyrinthe.core._

trait Wall extends Field {
  painter += "Fields/Wall"

  override def entering(context: MoveContext) = control {
    context.cancel()
  }
}
