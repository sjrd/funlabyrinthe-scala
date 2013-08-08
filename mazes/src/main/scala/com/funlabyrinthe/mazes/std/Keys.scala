package com.funlabyrinthe.mazes
package std

import com.funlabyrinthe.core._

trait Keys extends ItemDef {
  import universe._

  var lock: Lock = NoLock

  override def perform(player: Player) = {
    case OpenLock(l) if l == lock && (player has this) =>
      count(player) -= 1
  }
}
