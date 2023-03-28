package com.funlabyrinthe.mazes
package std

import cps.customValueDiscard

import com.funlabyrinthe.core._

trait Crossroads extends Effect {
  override def execute(context: MoveContext) = control {
    import context._
    goOnMoving = true
  }
}
