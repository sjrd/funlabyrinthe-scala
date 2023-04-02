package com.funlabyrinthe.editor.inspector

import com.funlabyrinthe.core.reflect._
import com.funlabyrinthe.editor.reflect._

import scala.collection.mutable

class InspectorRegistry extends TypeDirectedRegistry {
  import TypeDirectedRegistry.Entry._
  import RegistryEntry.{ ExactType, SubType, _ }

  type Entry = RegistryEntry

  PrimitiveEditors.registerPrimitiveEditors(this)

  def registerExactType(tpe: InspectedType, editorFactory: EditorFactory) =
    register(new ExactType(tpe, editorFactory))

  def registerExactTypeReadWrite(tpe: InspectedType, editorFactory: EditorFactory) =
    register(new ExactType(tpe, editorFactory) with ReadWriteOnly)

  def registerSubType(tpe: InspectedType, editorFactory: EditorFactory) =
    register(new SubType(tpe, editorFactory))

  def createEditor(inspector: Inspector, data: InspectedData): Option[Editor] = {
    findEntry(data) map (_.createEditor(inspector, data))
  }
}
