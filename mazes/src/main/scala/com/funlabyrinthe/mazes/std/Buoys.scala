package com.funlabyrinthe.mazes
package std

import cps.customValueDiscard

import com.funlabyrinthe.core.*
import com.funlabyrinthe.core.graphics.Painter
import com.funlabyrinthe.mazes.*

class Buoys(using ComponentInit) extends ItemDef {
  override def perform(player: Player) = {
    case GoOnWater if player has this =>
      control {
        player.plugins += Mazes.mazes.BuoyPlugin
      }
  }
}
