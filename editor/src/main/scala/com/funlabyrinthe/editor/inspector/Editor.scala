package com.funlabyrinthe.editor.inspector

import com.funlabyrinthe.core.reflect._

abstract class Editor(val inspector: Inspector, val data: InspectedData) {
  def name = data.name

  val isStringEditable: Boolean = false
  val hasValueList: Boolean = false
  val hasEditButton: Boolean = false
  val hasChildren: Boolean = false

  def valueString: String = data.valueString
  def valueString_=(v: String): Unit = {
    require(isStringEditable,
        s"Editor of class ${getClass.getName} does not support string editing")
    ??? // Must be overridden by subclasses
  }

  def valueList: List[Any] = {
    require(hasValueList,
        s"Editor of class ${getClass.getName} does not have a value list")
    ??? // Must be overridden by subclasses
  }

  def selectValueListItem(item: Any): Unit = {
    require(hasValueList,
        s"Editor of class ${getClass.getName} does not have a value list")
    ??? // Must be overridden by subclasses
  }

  def clickEditButton(): Unit = {
    require(hasEditButton,
        s"Editor of class ${getClass.getName} does not have an edit button")
    ??? // Must be overridden by subclasses
  }

  def children: List[Editor] = Nil
}
