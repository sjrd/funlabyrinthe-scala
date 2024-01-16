package com.funlabyrinthe.mazes
package std

import cps.customValueDiscard

import com.funlabyrinthe.core._
import com.funlabyrinthe.core.graphics.Painter

import Mazes.mazes

class Block(using ComponentInit) extends Obstacle {
  var lock: Lock = NoLock
  var message: String = ""

  override def pushing(context: MoveContext) = control {
    import context._

    cancel()

    if (keyEvent.isEmpty) {
      // Do nothing
    } else if (exec(player can OpenLock(lock))) {
      context.pos() += mazes.noObstacle
    } else {
      player.showMessage(message)
    }
  }
}

object Block:
  def make(name: String, painterItem: Painter.PainterItem, lock: Lock, message: String)(
      using ComponentInit): Block =

    val block = new Block
    block.name = name
    block.painter += painterItem
    block.lock = lock
    block.message = message
    block
  end make
end Block
