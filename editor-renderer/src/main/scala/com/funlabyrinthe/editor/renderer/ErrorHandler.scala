package com.funlabyrinthe.editor.renderer

import scala.scalajs.js.JavaScriptException

import com.raquo.airstream.core.Observer

final class ErrorHandler(onError: Observer[Throwable]):
  def handle(error: Throwable): Unit =
    error match
      case _: UserCancelException =>
        ()
      case _ =>
        error.printStackTrace()
        onError.onNext(error)
  end handle

  def handleErrors(op: => Unit): Unit =
    JSPI.async {
      try
        op
      catch case error: Throwable =>
        handle(error)
    }
  end handleErrors
end ErrorHandler

object ErrorHandler:
  def handleErrors(op: => Unit)(using handler: ErrorHandler): Unit =
    handler.handleErrors(op)

  def exceptionToString(exception: Throwable): String =
    try
      exception match
        case JavaScriptException(e) => e.toString()
        case _                      => exception.toString()
    catch case JavaScriptException(_) =>
      "Unknown error"
end ErrorHandler
