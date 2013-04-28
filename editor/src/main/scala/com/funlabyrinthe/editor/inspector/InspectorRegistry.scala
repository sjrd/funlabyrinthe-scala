package com.funlabyrinthe.editor.inspector

import scala.collection.mutable

import scala.reflect.runtime.universe._

class InspectorRegistry {
  import RegistryEntry._

  private var entries: List[RegistryEntry] = Nil

  PrimitiveEditors.registerPrimitiveEditors(this)
  register(new SubType(typeOf[AnyRef], new ClassMembersEditor(_, _), 10))

  def register(entry: RegistryEntry) {
    entries = entry :: entries
  }

  def registerExactType(tpe: Type, editorFactory: EditorFactory) =
    register(new ExactType(tpe, editorFactory))

  def registerSubType(tpe: Type, editorFactory: EditorFactory) =
    register(new SubType(tpe, editorFactory))

  def findEntry(data: InspectedData): Option[RegistryEntry] = {
    val allMatches = entries.filter(_.matches(data))
    if (allMatches.isEmpty) None
    else Some(allMatches.max(makeOrdering(data)))
  }

  def createEditor(inspector: Inspector, data: InspectedData): Option[Editor] = {
    findEntry(data) map (_.createEditor(inspector, data))
  }
}
