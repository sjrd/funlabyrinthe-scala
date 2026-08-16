package com.funlabyrinthe.mazes

import com.funlabyrinthe.core.*

class EnteredContext(val player: Player, val pos: SquareRef, val optSrc: Option[SquareRef])
    extends AbstractMoveContext {

  def this(player: Player, optSrc: Option[SquareRef]) = {
    this(player, player.position.getOrElse {
      throw IllegalArgumentException(s"Missing player position")
    }, optSrc)
  }

  val map: Map = pos.map

  var goOnMoving = false
}
