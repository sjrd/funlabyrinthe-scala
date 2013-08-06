package com.funlabyrinthe.mazes
package std

import com.funlabyrinthe.core._

trait Block extends Obstacle {
  import universe._
  import mazes._

  var lock: Lock = NoLock
  var message: String = ""

  override def pushing(context: MoveContext) {
    import context._

    cancel()

    if (keyEvent.isEmpty) {
      // Do nothing
    } else if (player can OpenLock(lock)) {
      context.pos() += NoObstacle
    } else {
      // TODO Show message
    }
  }
}
