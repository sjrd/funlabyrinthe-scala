package com.funlabyrinthe.mazes.std

import cps.customValueDiscard

import com.funlabyrinthe.core.*
import com.funlabyrinthe.mazes.*

class Water(using ComponentInit) extends Field {
  painter += "Fields/Water"

  override protected def doDraw(context: DrawSquareContext): Unit =
    super.doDraw(context)
    DissipateNeighbors.dissipateGroundNeighbors(context)
  end doDraw

  override def entering(context: MoveContext) = control {
    import context._

    if (exec(player cannot GoOnWater))
      cancel()
  }

  override def dispatch[A]: PartialFunction[SquareMessage[A], A] = {
    case PlankInteraction(PlankInteraction.Kind.PassOver, player, _, _, _) =>
      !player.isAbleTo(GoOnWater)
  }
}
