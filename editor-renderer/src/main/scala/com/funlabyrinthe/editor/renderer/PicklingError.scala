package com.funlabyrinthe.editor.renderer

import com.funlabyrinthe.coreinterface as intf

import com.raquo.laminar.api.L.{*, given}

import be.doeraene.webcomponents.ui5
import be.doeraene.webcomponents.ui5.configkeys.*

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

  def makeNotificationList(errors: List[PicklingError]): HtmlElement =
    ui5.NotificationList(
      errors.map { error =>
        val fullPath = error.component.fold(error.path)(_ :: error.path)
        ui5.NotificationList.item(
          _.titleText := error.message,
          fullPath.map(segment => ui5.NotificationListItem.slots.footnotes := span(segment)),
          _.state := ValueState.Negative,
        )
      },
    )
  end makeNotificationList
end PicklingError
