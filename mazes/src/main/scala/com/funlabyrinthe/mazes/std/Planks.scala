package com.funlabyrinthe.mazes.std

import com.funlabyrinthe.core.*
import com.funlabyrinthe.mazes.*

class Planks(using ComponentInit) extends ItemDef:
  name = "Planks"
  painter += "Objects/Plank"

  override protected def countChanged(player: CorePlayer, previousCount: Int, newCount: Int): Unit =
    if newCount > 0 then
      player.plugins += plankPlugin
    else
      player.plugins -= plankPlugin
end Planks
