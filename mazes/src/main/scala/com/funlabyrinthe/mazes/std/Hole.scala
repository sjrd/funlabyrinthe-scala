package com.funlabyrinthe.mazes.std

import com.funlabyrinthe.core.*
import com.funlabyrinthe.mazes.*

class Hole(using ComponentInit) extends Field {
  name = "Hole"
  painter += "Fields/Hole"

  var message: String = "Aren't you crazy for wanting to jump in that hole!?"

  override def entering(context: MoveContext) = {
    import context._

    cancel()
    player.showMessage(message)
  }

  override def dispatch[A]: PartialFunction[SquareMessage[A], A] = {
    case PlankInteraction(PlankInteraction.Kind.PassOver, _, _, _, _) =>
      true
  }
}
