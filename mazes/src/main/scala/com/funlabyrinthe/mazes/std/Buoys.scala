package com.funlabyrinthe.mazes.std

import com.funlabyrinthe.core.*
import com.funlabyrinthe.core.scene.Painter
import com.funlabyrinthe.mazes.*

class Buoys(using ComponentInit) extends ItemDef {
  icon += "Objects/Buoy"

  override def perform(player: CorePlayer) = {
    case GoOnWater if player has this =>
      player.plugins += buoyPlugin
  }
}
