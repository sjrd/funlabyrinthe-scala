package com.funlabyrinthe.mazes.std

import cps.customValueDiscard

import com.funlabyrinthe.core.*
import com.funlabyrinthe.mazes.*

class Crossroads(using ComponentInit) extends Effect {
  painter += "Arrows/Crossroads"

  override def execute(context: MoveContext) = control {
    import context._
    goOnMoving = true
  }
}
