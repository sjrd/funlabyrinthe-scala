package com.funlabyrinthe.mazes.std

import com.funlabyrinthe.core.*
import com.funlabyrinthe.core.graphics.Painter
import com.funlabyrinthe.mazes.*

class BuoyPlugin(using ComponentInit) extends PlayerPlugin {
  painterBefore += "Plugins/Buoy"

  override def perform(player: CorePlayer) = {
    case GoOnWater => ()
  }

  override def moved(context: MoveContext): Unit = {
    import context._

    if (!dest.map(_().field.isInstanceOf[Water]).getOrElse(false))
      player.plugins -= this
  }
}
