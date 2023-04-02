package com.funlabyrinthe.editor.pickling

import com.funlabyrinthe.core.reflect._

abstract class Pickler {
  def pickle(data: InspectedData)(implicit ctx: Context): Pickle

  def unpickle(data: InspectedData, pickle: Pickle)(
      implicit ctx: Context): Unit
}
