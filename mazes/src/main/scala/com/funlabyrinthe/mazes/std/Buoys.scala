package com.funlabyrinthe.mazes
package std

import cps.customValueDiscard

import com.funlabyrinthe.core._
import com.funlabyrinthe.core.graphics.Painter

class Buoys(using ComponentInit) extends ItemDef {
  def pluginPainter: Painter = Plugin.painterBefore
  def pluginPainter_=(value: Painter): Unit = Plugin.painterBefore = value

  override def perform(player: Player) = {
    case GoOnWater if player has this =>
      control {
        player.plugins += Plugin
      }
  }

  class Plugin private[Buoys] (using ComponentInit) extends PlayerPlugin {
    name = s"${Buoys.this.name} plugin"

    painterBefore += "Plugins/Buoy"

    override def perform(player: Player) = {
      case GoOnWater => doNothing()
    }

    override def moved(context: MoveContext) = control {
      import context._

      if (!dest.map(_().field.isInstanceOf[Water]).getOrElse(false))
        player.plugins -= this
    }
  }

  val Plugin = new Plugin(using summon[ComponentInit].withID(s"${Buoys.this.id}#Plugin"))
}
