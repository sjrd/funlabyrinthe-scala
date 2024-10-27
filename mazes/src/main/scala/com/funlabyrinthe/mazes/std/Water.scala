package com.funlabyrinthe.mazes.std

import com.funlabyrinthe.core.*
import com.funlabyrinthe.mazes.*

class Water(using ComponentInit) extends Field {
  painter += "Fields/Water"

  override protected def doDraw(context: DrawSquareContext): Unit =
    super.doDraw(context)
    DissipateNeighbors.dissipateGroundNeighbors(context)
  end doDraw

  override def entering(context: MoveContext): Unit = {
    import context._

    if (player cannot GoOnWater)
      cancel()
  }

  override def dispatch[A]: PartialFunction[SquareMessage[A], A] = {
    case PlankInteraction(PlankInteraction.Kind.PassOver, player, _, _, _) =>
      !player.isAbleTo(GoOnWater)
  }
}
