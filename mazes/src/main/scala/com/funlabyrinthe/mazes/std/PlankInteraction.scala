package com.funlabyrinthe.mazes.std

import com.funlabyrinthe.core.*
import com.funlabyrinthe.mazes.*

final case class PlankInteraction(
  kind: PlankInteraction.Kind,
  player: Player,
  passOverPos: SquareRef,
  leaveFrom: SquareRef,
  arriveAt: SquareRef,
) extends SquareMessage[Boolean]

object PlankInteraction:
  enum Kind:
    case PassOver, LeaveFrom, ArriveAt
end PlankInteraction
