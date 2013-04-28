package com.funlabyrinthe.editor.inspector

class ClassMembersEditor(inspector: Inspector, data: InspectedData)
extends Editor(inspector, data) with EditorWithReflectedMembers {

  override def valueString =
    "(" + data.value.getClass.getName + ")"
}
