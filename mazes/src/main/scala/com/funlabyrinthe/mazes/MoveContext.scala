package com.funlabyrinthe
package mazes

import core._
import graphics._
import input._

class MoveContext(val player: Player, val dest: Option[SquareRef[Map]],
    val keyEvent: Option[KeyEvent] = None) {

  private var _pos: SquareRef[Map] = _

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

  def cancel() {
    cancelled = true
  }

  def temporize(): Unit = Thread.sleep(temporization)
}
