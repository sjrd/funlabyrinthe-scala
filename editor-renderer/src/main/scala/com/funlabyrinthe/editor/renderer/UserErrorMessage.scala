package com.funlabyrinthe.editor.renderer

final class UserErrorMessage(
  message: String,
  cause: Throwable = null,
  val picklingErrors: List[PicklingError] = Nil,
) extends Exception(message, cause)
