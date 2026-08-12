package com.funlabyrinthe.mazes

import com.funlabyrinthe.core.*
import com.funlabyrinthe.core.input.*

class ExitingContext(val player: Player, val previousDirection: Direction,
    val dest: SquareRef, val keyEvent: Option[KeyEvent])
    extends AbstractMoveContext {

  require(player.position.exists(_.map == dest.map),
      s"Map mismatch: ${player.position}, $dest")

  val src: SquareRef = player.position.get
  val pos: SquareRef = src
  val map: Map = pos.map

  var canceled = false

  def cancel(): Unit =
    canceled = true
}
