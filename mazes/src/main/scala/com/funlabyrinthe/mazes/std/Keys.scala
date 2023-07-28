package com.funlabyrinthe.mazes
package std

import cps.customValueDiscard

import com.funlabyrinthe.core._

class Keys(using ComponentInit) extends ItemDef {
  import universe._

  var lock: Lock = NoLock

  override def perform(player: Player) = {
    case OpenLock(l) if l == lock && (player has this) =>
      control {
        count(player) -= 1
      }
  }
}
