package com.funlabyrinthe.editor.renderer

final class UserCancelException() extends Exception

object UserCancelException:
  def cancel(): Nothing =
    throw UserCancelException()
end UserCancelException
