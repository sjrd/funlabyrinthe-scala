package com.funlabyrinthe.mazes.std

import com.funlabyrinthe.core.*
import com.funlabyrinthe.mazes.*
import com.funlabyrinthe.core.scene.*

class Water(using ComponentInit) extends Field {
  painter += "Fields/Water"

  override protected def doPresent(context: PresentSquareContext): Batch[SceneNode] =
    super.doPresent(context) ++ DissipateNeighbors.presentDissipateGroundNeighbors(context)

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
