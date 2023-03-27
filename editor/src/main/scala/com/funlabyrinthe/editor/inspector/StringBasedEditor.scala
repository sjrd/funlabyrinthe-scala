package com.funlabyrinthe.editor.inspector

trait StringBasedEditor extends Editor {
  override val isStringEditable = !data.isReadOnly

  override def valueString_=(v: String): Unit = {
    data.value = stringToValue(v)
  }

  /** Convert a string entered by the user into a value of the data's type */
  def stringToValue(str: String): Any
}
