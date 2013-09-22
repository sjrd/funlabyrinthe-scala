package com.funlabyrinthe.editor.inspector

import com.funlabyrinthe.editor.reflect._
import ReflectionUtils._

import scala.reflect.runtime.universe._

object Utils {
  /** Enumerate the reflected data for properties of an instance */
  def reflectingEditorsForProperties(inspector: Inspector,
      instance: InstanceMirror, tpe: Type): Iterable[Editor] = {

    for {
      data <- reflectedDataForProperties(instance, tpe)
      editor <- inspector.registry.createEditor(inspector, data)
    } yield {
      editor
    }
  }
}
