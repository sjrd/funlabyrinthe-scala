package com.funlabyrinthe.mazes

abstract class AbstractMoveContext private[mazes] () {
  val player: Player
  val pos: SquareRef
  val map: Map

  var temporization = 500

  var hooked: Boolean = false

  def temporize(): Unit =
    player.sleep(temporization)
}
