package com.funlabyrinthe.editor.inspector

import com.funlabyrinthe.core.reflect._
import com.funlabyrinthe.editor.reflect._

abstract class RegistryEntry extends TypeDirectedRegistry.Entry {
  def createEditor(inspector: Inspector, data: InspectedData): Editor
}

trait RegistryEntryWithFactory extends RegistryEntry {
  protected val editorFactory: RegistryEntry.EditorFactory

  override def createEditor(inspector: Inspector, data: InspectedData) =
    editorFactory(inspector, data)
}

object RegistryEntry {
  import TypeDirectedRegistry.{ Entry => BaseEntry }

  type EditorFactory = (Inspector, InspectedData) => Editor

  class ExactType(val tpe: InspectedType, protected val editorFactory: EditorFactory,
      override protected val matchPercent0: Int = 90)
  extends RegistryEntryWithFactory with BaseEntry.ExactType

  class SubType(val tpe: InspectedType, protected val editorFactory: EditorFactory,
      override protected val matchPercent0: Int = 50)
  extends RegistryEntryWithFactory with BaseEntry.SubType
}
