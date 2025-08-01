package com.funlabyrinthe.corebridge

import scala.scalajs.js

object Errors:
  def protect[A](body: => A): A =
    try
      body
    catch
      case js.JavaScriptException(exception) =>
        js.special.`throw`(exception)
      case th: Throwable =>
        js.special.`throw`(new js.Error("" + th.getMessage()))
end Errors
