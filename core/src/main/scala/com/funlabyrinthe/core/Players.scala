package com.funlabyrinthe.core

trait Players { universe: Universe =>
  abstract class AbstractPlayer extends NamedComponent
                                   with VisualComponent {
  }

  type Player <: AbstractPlayer
}
