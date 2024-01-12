package com.funlabyrinthe.editor.inspector

import com.funlabyrinthe.core.inspecting
import com.funlabyrinthe.core.reflect._
import com.funlabyrinthe.core.Universe

object Utils {
  /** Enumerate the reflected data for properties of an instance */
  def reflectingEditorsForProperties(inspector: Inspector, instance: Any)(using Universe): Iterable[Editor] = {
    val propsData = instance match
      case instance: Reflectable =>
        instance.reflect().reflectProperties(instance)
      case _ =>
        Nil

    propsData.flatMap { propData =>
      propData.inspectable match {
        case None =>
          None

        case Some(inspectable) =>
          inspectable.editor match
            case inspecting.Editor.Int8 =>
              Some(new PrimitiveEditors.ByteEditor(inspector, propData))
            case inspecting.Editor.Int16 =>
              Some(new PrimitiveEditors.ShortEditor(inspector, propData))
            case inspecting.Editor.Int32 =>
              Some(new PrimitiveEditors.IntEditor(inspector, propData))
            case _: inspecting.Editor.SmallInteger =>
              None
            case inspecting.Editor.Text =>
              Some(new PrimitiveEditors.StringEditor(inspector, propData))
            case inspecting.Editor.Switch =>
              Some(new PrimitiveEditors.BooleanEditor(inspector, propData))
            case _ =>
              None
      }
    }
  }
}
