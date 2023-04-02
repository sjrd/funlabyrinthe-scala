package com.funlabyrinthe.editor.inspector

trait ValueListBasedEditor extends Editor {
  override val hasValueList = !data.isReadOnly

  def listItemToValue(item: Any): Any = item

  override def selectValueListItem(item: Any): Unit = {
    assert(valueList contains item,
        "Trying to select a value list item that is not in the list")
    data.asWritable.value = listItemToValue(item)
  }
}
