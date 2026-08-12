package com.funlabyrinthe.mazes

import com.funlabyrinthe.core.*

class ExitedContext(val player: Player, val pos: SquareRef)
    extends AbstractMoveContext {

  val optDest: Option[SquareRef] = player.position
  val map: Map = pos.map
}
