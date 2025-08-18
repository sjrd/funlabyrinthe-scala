package com.funlabyrinthe.core.pickling

import com.funlabyrinthe.core.Component

final class PicklingError(
  val component: Option[Component],
  val path: List[String],
  val message: String,
):
  override def toString(): String =
    val fullPath = component.fold(path)(_.id :: path)
    fullPath.mkString("", " > ", ": " + message)
  end toString
end PicklingError
