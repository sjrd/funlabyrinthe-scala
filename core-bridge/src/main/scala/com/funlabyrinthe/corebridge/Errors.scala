package com.funlabyrinthe.corebridge

import scala.scalajs.js
import scala.scalajs.js.JSConverters.*

import com.funlabyrinthe.core
import com.funlabyrinthe.coreinterface as intf

object Errors:
  def protect[A](body: => A): A =
    try
      body
    catch
      case js.JavaScriptException(exception) =>
        js.special.`throw`(exception)
      case th: Throwable =>
        js.special.`throw`(new js.Error("" + th.getMessage()))
  end protect

  def picklingErrorToIntf(coreError: core.pickling.PicklingError): intf.PicklingError =
    new intf.PicklingError {
      val component = coreError.component.map(_.id).orUndefined
      val path = coreError.path.toJSArray
      val message = coreError.message
    }
  end picklingErrorToIntf
end Errors
