package com.funlabyrinthe.coreinterface

import scala.scalajs.js

/** Services for editing APIs, typically to interact with the user. */
trait EditingServices extends js.Object:
  /** Mark the universe as modified.
   *
   *  Every editing action that modifies the universe in any way should call
   *  this method. The user will then see that something needs to be saved.
   */
  def markModified(): Unit

  /** Asks the user for confirmation before proceeding.
   *
   *  Displays an OK/Cancel-style dialog with the given message. Returns a
   *  `Promise` that completes with `true` if the user chooses "OK" and `false`
   *  otherwise.
   */
  def askConfirmation(message: String): js.Promise[Boolean]

  /** Silently cancels the current operation.
   *
   *  A cancellation exception gets thrown, so that the caller of this method
   *  does not proceed.
   *
   *  This method should typically be used when the user signalled their
   *  intention to cancel the operation.
   */
  def cancel(): Nothing

  /** Cancels the current operation and displays an error message to the user.
   *
   *  An error exception gets thrown, so that the caller of this method does
   *  not proceed.
   */
  def error(message: String): Nothing
end EditingServices
