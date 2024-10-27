package com.funlabyrinthe.mazes.std

import com.funlabyrinthe.core.*
import com.funlabyrinthe.mazes.*

class Crossroads(using ComponentInit) extends Effect {
  painter += "Arrows/Crossroads"

  override def execute(context: MoveContext): Unit = {
    import context._
    goOnMoving = true
  }
}
