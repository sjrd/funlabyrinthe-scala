package com.funlabyrinthe.editor.inspector

trait StringBasedEditor extends Editor {
  override val isStringEditable = true

  override def valueString_=(v: String) {
    data.value = stringToValue(v)
  }

  /** Convert a string entered by the user into a value of the data's type */
  def stringToValue(str: String): Any
}
