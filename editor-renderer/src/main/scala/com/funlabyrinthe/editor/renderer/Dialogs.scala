package com.funlabyrinthe.editor.renderer

import scala.scalajs.js

import com.raquo.airstream.core.Observer

final class Dialogs(
  askConfirmationWriter: Observer[(String, Boolean => Unit)]
)(using ErrorHandler):
  def askConfirmation(message: String): Boolean =
    JSPI.await(new js.Promise[Boolean]({ (resolve, failure) =>
      askConfirmationWriter.onNext((message, result => resolve(result)))
    }))
  end askConfirmation
end Dialogs

object Dialogs:
  def askConfirmation(message: String)(using dialogs: Dialogs): Boolean =
    dialogs.askConfirmation(message)
end Dialogs
