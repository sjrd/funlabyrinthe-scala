package com.funlabyrinthe.mazes

import com.funlabyrinthe.core.*

class ExecuteContext(val player: Player, val pos: SquareRef)
    extends AbstractMoveContext {

  def this(player: Player) = {
    this(player, player.position.getOrElse {
      throw IllegalArgumentException(s"Missing player position")
    })
  }

  val map: Map = pos.map

  var goOnMoving = false
}
