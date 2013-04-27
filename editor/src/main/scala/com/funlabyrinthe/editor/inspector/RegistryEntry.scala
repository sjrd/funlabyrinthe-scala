package com.funlabyrinthe.editor.inspector

import scala.reflect.runtime.universe._

abstract class RegistryEntry {
  def matches(data: InspectedData): Boolean
  def matchPercent(data: InspectedData): Int

  def compareTo(that: RegistryEntry, data: InspectedData): Int = {
    assert(this.matches(data) && that.matches(data))
    this.matchPercent(data) - that.matchPercent(data)
  }

  def createEditor(inspector: Inspector, data: InspectedData): Editor
}

trait RegistryEntryWithFactory extends RegistryEntry {
  protected val editorFactory: RegistryEntry.EditorFactory

  override def createEditor(inspector: Inspector, data: InspectedData) =
    editorFactory(inspector, data)
}

object RegistryEntry {
  type EditorFactory = (Inspector, InspectedData) => Editor

  def makeOrdering(data: InspectedData): Ordering[RegistryEntry] = {
    new EntryOrdering(data)
  }

  private class EntryOrdering(val data: InspectedData)
  extends Ordering[RegistryEntry] {

    def compare(left: RegistryEntry, right: RegistryEntry): Int =
      left.compareTo(right, data)
  }

  class ExactType(val tpe: Type, protected val editorFactory: EditorFactory,
      matchPercent0: Int = 90)
  extends RegistryEntryWithFactory {

    override def matches(data: InspectedData) = data.tpe =:= tpe
    override def matchPercent(data: InspectedData) = matchPercent0
  }

  class SubType(val tpe: Type, protected val editorFactory: EditorFactory,
      matchPercent0: Int = 50)
  extends RegistryEntryWithFactory {

    override def matches(data: InspectedData) = data.tpe <:< tpe
    override def matchPercent(data: InspectedData) = matchPercent0
  }
}
