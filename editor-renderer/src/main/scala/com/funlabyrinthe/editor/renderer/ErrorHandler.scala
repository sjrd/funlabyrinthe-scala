package com.funlabyrinthe.editor.renderer

import scala.concurrent.{ExecutionContext, Future}

import scala.scalajs.js.JavaScriptException

import com.raquo.airstream.core.Observer

final class ErrorHandler(onError: Observer[Throwable]):
  def handle(error: Throwable): Unit =
    error.printStackTrace()
    onError.onNext(error)

  def handleErrors(op: Future[Unit])(using ExecutionContext): Unit =
    op.recover(handle(_))

  def handleErrorsSync(op: => Unit): Unit =
    try
      op
    catch case error: Throwable =>
      handle(error)
  end handleErrorsSync
end ErrorHandler

object ErrorHandler:
  def handle(error: Throwable)(using handler: ErrorHandler): Unit =
    handler.handle(error)

  def handleErrors(op: Future[Unit])(using handler: ErrorHandler, ec: ExecutionContext): Unit =
    handler.handleErrors(op)

  def handleErrorsSync(op: => Unit)(using handler: ErrorHandler): Unit =
    handler.handleErrorsSync(op)

  def exceptionToString(exception: Throwable): String =
    try
      exception.toString()
    catch case JavaScriptException(_) =>
      "Unknown error"
end ErrorHandler
