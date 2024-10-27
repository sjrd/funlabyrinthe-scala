package com.funlabyrinthe.mazes

import com.funlabyrinthe.core.*
import com.funlabyrinthe.core.graphics.*
import com.funlabyrinthe.core.input.*

class MoveContext(val player: Player, val dest: Option[SquareRef],
    val keyEvent: Option[KeyEvent] = None) {

  private var _pos: SquareRef = scala.compiletime.uninitialized

  val src = player.position
  def pos = _pos
  val oldDirection = player.direction

  def setPosToSource() = _pos = src.get
  def setPosToDest() = _pos = dest.get

  def isRegular =
    src.isDefined && dest.isDefined && (src.get.map eq dest.get.map)

  var cancelled = false
  var goOnMoving = false
  var hooked = false

  var temporization = 500

  def cancel(): Unit = {
    cancelled = true
  }

  def temporize(): Control[Unit] = control {
    player.sleep(temporization)
  }
}
