package com.funlabyrinthe.editor.renderer.model

final case class ProjectID(id: String):
  import ProjectID.*

  require(isValidProjectID(id), s"Not a valid project ID: '$id'")

  override def toString(): String = id
end ProjectID

object ProjectID:
  def isValidProjectID(id: String): Boolean =
    import UserID.*

    id match
      case s"$ownerID/$simpleID" =>
        isValidUserID(ownerID) && isValidUserID(simpleID)
      case _ =>
        false
  end isValidProjectID

  given ProjectIDOrdering: Ordering[ProjectID] = Ordering.by(_.id)
end ProjectID
