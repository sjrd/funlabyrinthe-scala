package com.funlabyrinthe.editor.inspector

import com.funlabyrinthe.editor.reflect._

class ClassMembersEditor(inspector: Inspector, data: InspectedData)
extends Editor(inspector, data) with EditorWithReflectedMembers {

  override def valueString =
    "(" + data.value.getClass.getName + ")"
}
