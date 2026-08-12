package com.funlabyrinthe.mazes

import com.funlabyrinthe.core.*

class EnteredContext(val player: Player, val optSrc: Option[SquareRef])
    extends AbstractMoveContext {

  require(player.position.isDefined, s"Missing player position")

  val pos = player.position.get
  val map: Map = pos.map

  var goOnMoving = false
}
