package com.funlabyrinthe.editor.inspector

/** Triggered when the string entered by the user is not valid for the type */
class ValueFormatException(message: String, cause: Throwable)
extends IllegalArgumentException(message, cause) {

  def this() = this(null, null)
  def this(message: String) = this(message, null)
  def this(cause: Throwable) = this(null, cause)
}
