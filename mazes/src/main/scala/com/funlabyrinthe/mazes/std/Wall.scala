package com.funlabyrinthe.mazes.std

import com.funlabyrinthe.core.*
import com.funlabyrinthe.mazes.*

class Wall(using ComponentInit) extends Field {
  painter += "Fields/Wall"

  override def entering(context: MoveContext): Unit = {
    context.cancel()
  }
}
