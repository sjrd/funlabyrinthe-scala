package com.funlabyrinthe.mazes
package std

import com.funlabyrinthe.core._

trait MessagesPlugin extends PlayerPlugin {
  def showMessage(player: Player, message: String): Boolean @control

  override def onMessage(player: Player, message: Any): Boolean @control = {
    // Word around a continutions plugin warning
    /*message match {
      case ShowMessage(msg) => showMessage(player, msg)
      case _ => super.onMessage(player, message)
    }*/
    if (message.isInstanceOf[ShowMessage])
      showMessage(player, message.asInstanceOf[ShowMessage].message)
    else
      super.onMessage(player, message)
  }
}
