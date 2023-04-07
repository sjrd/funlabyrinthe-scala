package com.funlabyrinthe.editor.pickling

import com.funlabyrinthe.core.reflect._

object Utils {
  /** Enumerate the reflected data for properties of an instance */
  def reflectingPicklersForProperties(instance: Any,
      bestKnownSuperType: InspectedType)(
      implicit ctx: Context): Iterable[(InspectedData, Pickler)] = {

    val propsData = instance match
      case instance: Reflectable =>
        instance.reflect().reflectProperties(instance)
      case _ =>
        Nil

    for
      data <- propsData
      pickler <- ctx.registry.createPickler(data)
    yield
      (data, pickler)
  }
}
