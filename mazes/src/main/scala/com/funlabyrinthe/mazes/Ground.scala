package com.funlabyrinthe.mazes

import com.funlabyrinthe.core.*
import com.funlabyrinthe.mazes.std.*

abstract class Ground(using ComponentInit) extends Field:
  override def dispatch[A]: PartialFunction[SquareMessage[A], A] = {
    case PlankInteraction(PlankInteraction.Kind.LeaveFrom, _, _, leaveFrom, arriveAt) =>
      arriveAt().field.isInstanceOf[Ground]
        && leaveFrom().obstacle == noObstacle
        && arriveAt().obstacle == noObstacle
  }
end Ground
