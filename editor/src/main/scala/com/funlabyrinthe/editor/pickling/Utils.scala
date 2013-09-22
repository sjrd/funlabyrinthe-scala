package com.funlabyrinthe.editor.pickling

import com.funlabyrinthe.editor.reflect._
import ReflectionUtils._

import scala.reflect.runtime.universe._

object Utils {
  /** Enumerate the reflected data for properties of an instance */
  def reflectingPicklersForProperties(instance: InstanceMirror,
      tpe: Type)(implicit ctx: Context): Iterable[(InspectedData, Pickler)] = {

    for {
      data <- reflectedDataForProperties(instance, tpe)
      editor <- ctx.registry.createPickler(data)
    } yield {
      (data, editor)
    }
  }

  /** Enumerate the reflected data for fields of an instance */
  def reflectingPicklersForFields(instance: InstanceMirror,
      tpe: Type)(implicit ctx: Context): Iterable[(InspectedData, Pickler)] = {

    for {
      data <- reflectedDataForFields(instance, tpe)
      editor <- ctx.registry.createPickler(data)
    } yield {
      (data, editor)
    }
  }
}
