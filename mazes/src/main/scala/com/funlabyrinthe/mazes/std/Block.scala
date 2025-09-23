package com.funlabyrinthe.mazes.std

import scala.Conversion.into

import com.funlabyrinthe.core.*
import com.funlabyrinthe.core.graphics.Painter

import com.funlabyrinthe.mazes.*

class Block(using ComponentInit) extends Obstacle {
  var lock: Lock = Lock.NoLock
  var message: String = ""

  hideEffectAndTool = true

  override def pushing(context: MoveContext): Unit = {
    import context._

    cancel()

    if (keyEvent.isEmpty) {
      // Do nothing
    } else if (player can OpenLock(lock)) {
      context.pos() += noObstacle
    } else {
      player.showMessage(message)
    }
  }
}

object Block:
  def make(painterItem: into[Painter.PainterItem], lock: Lock, message: String)(using ComponentInit): Block =
    val block = new Block
    block.painter += painterItem
    block.lock = lock
    block.message = message
    block
  end make
end Block
