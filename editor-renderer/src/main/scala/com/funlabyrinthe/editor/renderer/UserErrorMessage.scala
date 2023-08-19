package com.funlabyrinthe.editor.renderer

final class UserErrorMessage(message: String, cause: Throwable) extends Exception:
  def this(message: String) = this(message, null)
  def this(cause: Throwable) = this(cause.getMessage(), cause)
end UserErrorMessage
