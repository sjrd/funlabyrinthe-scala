package com.funlabyrinthe.core

/** The result of a user action while editing the universe. */
enum EditUserActionResult:
  /** The requested action was successfully performed.
   *
   *  Consider the universe to be changed.
   */
  case Done

  /** The requested action was successful but did not lead to any change.
   *
   *  Consider the universe to be unchanged.
   */
  case Unchanged

  /** The requested action could not be performed and resulted in an error.
   *
   *  The specified message will be displayed to the user.
   */
  case Error(message: String)

  /** Asks the user for confirmation before proceeding.
   *
   *  Displays an OK/Cancel-style dialog with the given message. If the
   *  user chooses "Cancel" or otherwise indicates cancellation (such as
   *  through the "Escape" key or clicking outside the dialog)), nothing else
   *  happens and the universe is considered to be unchanged. If they choose
   *  "OK", call `onConfirm()` to proceed.
   */
  case AskConfirmation(message: String, onConfirm: () => EditUserActionResult)

  /** Sequence of two actions.
   *
   *  The second action is performed only if the first one is successful.
   */
  case Sequence(first: EditUserActionResult, second: () => EditUserActionResult)

  /** This action result, followed by `after` if this is successful. */
  def andThen(after: => EditUserActionResult): EditUserActionResult =
    Sequence(this, () => after)
end EditUserActionResult
