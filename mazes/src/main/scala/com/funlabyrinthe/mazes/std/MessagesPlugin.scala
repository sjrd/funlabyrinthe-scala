package com.funlabyrinthe.mazes
package std

import com.funlabyrinthe.core._

trait MessagesPlugin extends PlayerPlugin {
  def showMessage(player: Player, message: String): Boolean @control

  override def onMessage(player: Player, message: Any) = message match {
    case ShowMessage(msg) => showMessage(player, msg)
    case _ => super.onMessage(player, message)
  }
}
