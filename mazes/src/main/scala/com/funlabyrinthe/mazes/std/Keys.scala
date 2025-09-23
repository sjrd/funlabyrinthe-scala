package com.funlabyrinthe.mazes.std

import scala.Conversion.into

import com.funlabyrinthe.core.*
import com.funlabyrinthe.core.graphics.Painter

class Keys(using ComponentInit) extends ItemDef {
  import universe._

  var lock: Lock = Lock.NoLock

  override def perform(player: CorePlayer) = {
    case OpenLock(l) if l == lock && (player has this) =>
      count(player) -= 1
  }
}

object Keys:
  def make(iconItem: into[Painter.PainterItem], lock: Lock)(using ComponentInit): Keys =
    val keys = new Keys
    keys.icon += iconItem
    keys.lock = lock
    keys
  end make
end Keys
