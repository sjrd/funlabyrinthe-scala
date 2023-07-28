package com.funlabyrinthe.mazes
package std

import cps.customValueDiscard

import com.funlabyrinthe.core._

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
      context.pos() += mazes.NoObstacle
    } else {
      player.showMessage(message)
    }
  }
}
