package com.funlabyrinthe.editor.common.model

final case class UserID(id: String):
  import UserID.*

  require(isValidUserID(id), s"Not a valid user ID: '$id'")

  override def toString(): String = id
end UserID

object UserID:
  def isLetter(c: Char): Boolean = c >= 'a' && c <= 'z'
  def isLetterOrDigit(c: Char): Boolean = isLetter(c) || (c >= '0' && c <= '9')

  def isValidUserID(id: String): Boolean =
    !id.isEmpty()
      && isLetter(id.head)
      && id.tail.forall(isLetterOrDigit(_))
  end isValidUserID
end UserID
