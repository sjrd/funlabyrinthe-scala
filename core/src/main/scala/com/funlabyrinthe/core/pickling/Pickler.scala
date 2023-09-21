package com.funlabyrinthe.core.pickling

import com.funlabyrinthe.core.reflect._

abstract class Pickler {
  def pickle(data: InspectedData)(implicit ctx: PicklingContext): Pickle

  def unpickle(data: InspectedData, pickle: Pickle)(
      implicit ctx: PicklingContext): Unit
}
