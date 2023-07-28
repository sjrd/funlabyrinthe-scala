package com.funlabyrinthe.mazes.std

import com.funlabyrinthe.core.*
import com.funlabyrinthe.mazes.*

class Planks(using ComponentInit) extends ItemDef:
  override protected def countChanged(player: Player, previousCount: Int, newCount: Int): Unit =
    if newCount > 0 then
      player.plugins += Mazes.mazes.PlankPlugin
    else
      player.plugins -= Mazes.mazes.PlankPlugin
end Planks
