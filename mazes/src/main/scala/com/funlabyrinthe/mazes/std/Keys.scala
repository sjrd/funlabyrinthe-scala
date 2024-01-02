package com.funlabyrinthe.mazes
package std

import cps.customValueDiscard

import com.funlabyrinthe.core._
import com.funlabyrinthe.core.graphics.Painter

class Keys(using ComponentInit) extends ItemDef {
  import universe._

  var lock: Lock = NoLock

  override def perform(player: CorePlayer) = {
    case OpenLock(l) if l == lock && (player has this) =>
      control {
        count(player) -= 1
      }
  }
}

object Keys:
  def make(name: String, painterItem: Painter.PainterItem, lock: Lock)(using ComponentInit): Keys =
    val keys = new Keys
    keys.name = name
    keys.painter += painterItem
    keys.lock = lock
    keys
  end make
end Keys
