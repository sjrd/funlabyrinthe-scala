package com.funlabyrinthe.core.pickling

import com.funlabyrinthe.core.Universe

trait Context {
  val universe: Universe
}

object Context:
  def make(universe: Universe): Context =
    val universe0 = universe
    new Context {
      val universe: Universe = universe0
    }
  end make
end Context
