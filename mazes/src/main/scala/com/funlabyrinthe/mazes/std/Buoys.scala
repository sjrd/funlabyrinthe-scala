package com.funlabyrinthe.mazes
package std

import com.funlabyrinthe.core._

trait Buoys extends ItemDef {
  import universe._
  import mazes._

  def pluginPainter: Painter = Plugin.painterBefore
  def pluginPainter_=(value: Painter): Unit = Plugin.painterBefore = value

  override def perform(player: Player) = {
    case GoOnWater if player has this =>
      player.plugins += Plugin
  }

  class Plugin private[Buoys] () extends PlayerPlugin(
      ComponentID(s"${Buoys.this.id}#Plugin")) {
    name = s"${Buoys.this.name} plugin"

    painterBefore += "Plugins/Buoy"

    override def perform(player: Player) = {
      case GoOnWater => ()
    }

    override def moved(context: MoveContext) = {
      import context._

      if (!dest.map(_().field.isInstanceOf[Water]).getOrElse(false))
        player.plugins -= this
    }
  }

  val Plugin = new Plugin
}
