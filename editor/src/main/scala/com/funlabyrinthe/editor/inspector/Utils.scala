package com.funlabyrinthe.editor.inspector

import com.funlabyrinthe.core.reflect._
import com.funlabyrinthe.editor.reflect._

object Utils {
  /** Enumerate the reflected data for properties of an instance */
  def reflectingEditorsForProperties(inspector: Inspector, instance: Any,
      bestKnownSuperType: InspectedType = InspectedType.Any): Iterable[Editor] = {

    val propsData = instance match
      case instance: Reflectable =>
        instance.reflect().reflectProperties(instance)
      case _ =>
        Nil

    propsData.flatMap(inspector.registry.createEditor(inspector, _))
  }
}
