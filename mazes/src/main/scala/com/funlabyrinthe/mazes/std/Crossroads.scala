package com.funlabyrinthe.mazes
package std

import cps.customValueDiscard

import com.funlabyrinthe.core._

class Crossroads(using ComponentInit) extends Effect {
  name = "Crossroads"
  painter += "Arrows/Crossroads"

  override def execute(context: MoveContext) = control {
    import context._
    goOnMoving = true
  }
}
