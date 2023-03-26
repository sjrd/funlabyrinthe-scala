package com.funlabyrinthe.editor.pickling

import com.funlabyrinthe.editor.reflect._

object Utils {
  /** Enumerate the reflected data for properties of an instance */
  def reflectingPicklersForProperties(instance: Any,
      bestKnownSuperType: InspectedType)(
      implicit ctx: Context): Iterable[(InspectedData, Pickler)] = {

    for {
      data <- ReflectionUtils.reflectedDataForProperties(instance, bestKnownSuperType)
      editor <- ctx.registry.createPickler(data)
    } yield {
      (data, editor)
    }
  }

  /** Enumerate the reflected data for fields of an instance */
  def reflectingPicklersForFields(instance: Any,
      bestKnownSuperType: InspectedType)(
      implicit ctx: Context): Iterable[(InspectedData, Pickler)] = {

    for {
      data <- ReflectionUtils.reflectedDataForFields(instance, bestKnownSuperType)
      editor <- ctx.registry.createPickler(data)
    } yield {
      (data, editor)
    }
  }
}
