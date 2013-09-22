package com.funlabyrinthe.editor.inspector

import com.funlabyrinthe.editor.reflect._

import scala.collection.mutable

import scala.reflect.runtime.universe._

class InspectorRegistry extends TypeDirectedRegistry {
  import TypeDirectedRegistry.Entry._
  import RegistryEntry.{ ExactType, SubType, _ }

  type Entry = RegistryEntry

  PrimitiveEditors.registerPrimitiveEditors(this)

  def registerExactType(tpe: Type, editorFactory: EditorFactory) =
    register(new ExactType(tpe, editorFactory))

  def registerExactTypeReadWrite(tpe: Type, editorFactory: EditorFactory) =
    register(new ExactType(tpe, editorFactory) with ReadWriteOnly)

  def registerSubType(tpe: Type, editorFactory: EditorFactory) =
    register(new SubType(tpe, editorFactory))

  def createEditor(inspector: Inspector, data: InspectedData): Option[Editor] = {
    findEntry(data) map (_.createEditor(inspector, data))
  }
}
