package com.funlabyrinthe.mazes.std

import cps.customValueDiscard

import com.funlabyrinthe.core.*
import com.funlabyrinthe.core.graphics.Painter

import com.funlabyrinthe.mazes.*

class Block(using ComponentInit) extends Obstacle {
  var lock: Lock = NoLock
  var message: String = ""

  hideEffectAndTool = true

  override def pushing(context: MoveContext) = control {
    import context._

    cancel()

    if (keyEvent.isEmpty) {
      // Do nothing
    } else if (exec(player can OpenLock(lock))) {
      context.pos() += noObstacle
    } else {
      player.showMessage(message)
    }
  }
}

object Block:
  def make(painterItem: Painter.PainterItem, lock: Lock, message: String)(using ComponentInit): Block =
    val block = new Block
    block.painter += painterItem
    block.lock = lock
    block.message = message
    block
  end make
end Block
