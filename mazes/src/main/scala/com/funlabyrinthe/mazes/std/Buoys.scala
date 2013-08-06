package com.funlabyrinthe.mazes
package std

trait Buoys extends ItemDef {
  import universe._
  import mazes._

  def pluginPainter: Painter = Plugin.painterBefore
  def pluginPainter_=(value: Painter): Unit = Plugin.painterBefore = value

  override def perform(player: Player) = super.perform(player) orElse {
    case GoOnWater if player has this =>
      player.plugins += Plugin
  }

  object Plugin extends PlayerPlugin {
    id = s"${Buoys.this.id}#Plugin"
    name = s"${Buoys.this.name} plugin"

    painterBefore += "Plugins/Buoy"

    override def perform(player: Player) = super.perform(player) orElse {
      case GoOnWater => ()
    }

    override def moved(context: MoveContext) = {
      import context._

      if (!dest.map(_().field.isInstanceOf[Water]).getOrElse(false))
        player.plugins -= this
    }
  }
}
