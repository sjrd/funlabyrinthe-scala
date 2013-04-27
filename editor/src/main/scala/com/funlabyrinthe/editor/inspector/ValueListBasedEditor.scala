package com.funlabyrinthe.editor.inspector

trait ValueListBasedEditor extends Editor {
  override val hasValueList = true

  def listItemToValue(item: Any): Any = item

  override def selectValueListItem(item: Any) {
    assert(valueList contains item,
        "Trying to select a value list item that is not in the list")
    data.value = listItemToValue(item)
  }
}
