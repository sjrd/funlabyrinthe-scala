package com.funlabyrinthe
package mazes

import com.funlabyrinthe.core._
import com.funlabyrinthe.core.graphics._
import com.funlabyrinthe.mazes.std.PlankInteraction

abstract class Ground(using ComponentInit) extends Field:
  override def dispatch[A]: PartialFunction[SquareMessage[A], A] = {
    case PlankInteraction(PlankInteraction.Kind.LeaveFrom, _, _, leaveFrom, arriveAt) =>
      arriveAt().field.isInstanceOf[Ground]
        && leaveFrom().obstacle == Mazes.mazes.NoObstacle
        && arriveAt().obstacle == Mazes.mazes.NoObstacle
  }
end Ground
