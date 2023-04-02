package com.funlabyrinthe.editor.inspector

import com.funlabyrinthe.core.reflect._

trait EditorWithReflectedMembers extends EditorWithLazyMembers {
  require(data.tpe.isSubtype(InspectedType.AnyRef),
      s"EditorWithReflectedMembers requires an AnyRef type, found ${data.tpe}")

  override def complete(): Unit = {
    childEditors ++= Utils.reflectingEditorsForProperties(inspector, data.value, data.tpe)
  }
}
