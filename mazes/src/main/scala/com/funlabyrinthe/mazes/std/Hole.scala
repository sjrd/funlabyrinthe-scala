package com.funlabyrinthe.mazes.std

import com.funlabyrinthe.core.*
import com.funlabyrinthe.mazes.*
import com.funlabyrinthe.core.scene.*

class Hole(using ComponentInit) extends Field {
  painter += "Fields/Hole"

  var message: String = "We cannot see the bottom of that hole. Better not fall into it."

  override protected def doPresent(context: PresentSquareContext): Batch[SceneNode] =
    super.doPresent(context) ++ DissipateNeighbors.presentDissipateGroundNeighbors(context)

  override def entering(context: EnteringContext): Unit = {
    import context._

    cancel()
    player.showMessageOnce(message)
  }

  override def dispatch[A]: PartialFunction[SquareMessage[A], A] = {
    case PlankInteraction(PlankInteraction.Kind.PassOver, _, _, _, _) =>
      true
  }
}
