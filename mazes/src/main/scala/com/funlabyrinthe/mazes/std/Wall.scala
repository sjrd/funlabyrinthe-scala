package com.funlabyrinthe.mazes.std

import cps.customValueDiscard

import com.funlabyrinthe.core.*
import com.funlabyrinthe.mazes.*

class Wall(using ComponentInit) extends Field {
  name = "Wall"
  painter += "Fields/Wall"

  override def entering(context: MoveContext) = control {
    context.cancel()
  }
}
