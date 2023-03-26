package com.funlabyrinthe.editor.inspector

import com.funlabyrinthe.editor.reflect._

trait EditorWithReflectedMembers extends EditorWithLazyMembers {
  require(data.tpe.isSubtype(InspectedType.AnyRef),
      s"EditorWithReflectedMembers requires an AnyRef type, found ${data.tpe}")

  override def complete() {
    childEditors ++= Utils.reflectingEditorsForProperties(inspector, data.value, data.tpe)
  }
}
