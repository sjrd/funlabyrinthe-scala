package com.funlabyrinthe.core

/** Services for editing APIs, typically to interact with the user. */
trait EditingServices:
  /** Mark the universe as modified.
   *
   *  Every editing action that modifies the universe in any way should call
   *  this method. The user will then see that something needs to be saved.
   */
  def markModified(): Unit

  /** Asks the user for confirmation before proceeding.
   *
   *  Displays an OK/Cancel-style dialog with the given message.
   *
   *  If the user chooses "Cancel" or otherwise indicates cancellation (such as
   *  through the "Escape" key or clicking outside the dialog)), this method
   *  returns `false`. Caller code should typically follow up with a call to
   *  `cancel()` to cancel the ongoing operation.
   *
   *  If they choose "OK", this method returns `true`.
   */
  def askConfirmation(message: String): Boolean

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

object EditingServices:
  /** Mark the universe as modified.
   *
   *  Every editing action that modifies the universe in any way should call
   *  this method. The user will then see that something needs to be saved.
   */
  def markModified()(using EditingServices): Unit =
    summon[EditingServices].markModified()

  /** Asks the user for confirmation before proceeding.
   *
   *  Displays an OK/Cancel-style dialog with the given message.
   *
   *  If the user chooses "Cancel" or otherwise indicates cancellation (such as
   *  through the "Escape" key or clicking outside the dialog)), this method
   *  returns `false`. Caller code should typically follow up with a call to
   *  `cancel()` to cancel the ongoing operation.
   *
   *  If they choose "OK", this method returns `true`.
   */
  def askConfirmation(message: String)(using EditingServices): Boolean =
    summon[EditingServices].askConfirmation(message)

  /** Asks the user for confirmation before proceeding.
   *
   *  Displays an OK/Cancel-style dialog with the given message.
   *
   *  If the user chooses "Cancel" or otherwise indicates cancellation (such as
   *  through the "Escape" key or clicking outside the dialog)), the operation
   *  gets canceled as if by calling `cancel()`.
   *
   *  If they choose "OK", this method returns normally and the caller code
   *  proceeds.
   */
  def askConfirmationOrCancel(message: String)(using EditingServices): Unit =
    if !askConfirmation(message) then
      cancel()

  /** Silently cancels the current operation.
   *
   *  A cancellation exception gets thrown, so that the caller of this method
   *  does not proceed.
   *
   *  This method should typically be used when the user signalled their
   *  intention to cancel the operation.
   */
  def cancel()(using EditingServices): Nothing =
    summon[EditingServices].cancel()

  /** Cancels the current operation and displays an error message to the user.
   *
   *  An error exception gets thrown, so that the caller of this method does
   *  not proceed.
   */
  def error(message: String)(using EditingServices): Nothing =
    summon[EditingServices].error(message)
end EditingServices
