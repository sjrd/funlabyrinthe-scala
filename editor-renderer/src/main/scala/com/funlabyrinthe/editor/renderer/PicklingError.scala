package com.funlabyrinthe.editor.renderer

import com.funlabyrinthe.coreinterface as intf

final class PicklingError(
  val component: Option[String],
  val path: List[String],
  val message: String,
):
  override def toString(): String =
    val fullPath = component.fold(path)(_ :: path)
    fullPath.mkString("", " > ", ": " + message)
  end toString
end PicklingError

object PicklingError:
  def fromInterface(intfError: intf.PicklingError): PicklingError =
    PicklingError(
      intfError.component.toOption,
      intfError.path.toList,
      intfError.message,
    )
  end fromInterface
end PicklingError
