package com.funlabyrinthe.editor.pickling

import com.funlabyrinthe.core.reflect._
import com.funlabyrinthe.editor.reflect._

trait MutableMembersPickler extends Pickler {
  val tpe: InspectedType

  def pickle(data: InspectedData)(implicit ctx: Context): Pickle =
    InPlacePickleable.ForReflectable.pickle(data.value.asInstanceOf[Reflectable])

  def unpickle(data: InspectedData, pickle: Pickle)(
      implicit ctx: Context): Unit = {
    InPlacePickleable.ForReflectable.unpickle(data.value.asInstanceOf[Reflectable], pickle)
  }
}
