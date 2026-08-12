package com.funlabyrinthe.mazes.std

import com.funlabyrinthe.core.*
import com.funlabyrinthe.core.scene.Painter
import com.funlabyrinthe.mazes.*

class BuoyPlugin(using ComponentInit) extends PlayerPlugin {
  painterUnder += "Plugins/Buoy"

  override def perform(player: CorePlayer) = {
    case GoOnWater => ()
  }

  override def exited(context: ExitedContext): Unit = {
    import context._

    if !optDest.exists(_().field.isInstanceOf[Water]) then
      player.plugins -= this
  }
}
