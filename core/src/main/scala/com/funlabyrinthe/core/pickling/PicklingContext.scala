package com.funlabyrinthe.core.pickling

import com.funlabyrinthe.core.Universe

trait PicklingContext {
  val universe: Universe
}

object PicklingContext:
  def make(universe: Universe): PicklingContext =
    val universe0 = universe
    new PicklingContext {
      val universe: Universe = universe0
    }
  end make
end PicklingContext
