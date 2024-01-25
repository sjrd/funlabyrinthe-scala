package com.funlabyrinthe.editor.renderer

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import com.raquo.airstream.core.Observer

final class Dialogs(
  askConfirmationWriter: Observer[(String, () => Future[Unit])]
)(using ErrorHandler):
  def askConfirmation(message: String)(onConfirm: => Future[Unit]): Unit =
    askConfirmationWriter.onNext((message, () => onConfirm))
end Dialogs

object Dialogs:
  def askConfirmation(message: String)(onConfirm: => Future[Unit])(using dialogs: Dialogs): Unit =
    dialogs.askConfirmation(message)(onConfirm)
end Dialogs
