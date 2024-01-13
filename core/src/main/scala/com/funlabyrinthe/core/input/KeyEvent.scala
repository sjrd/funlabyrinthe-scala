package com.funlabyrinthe.core.input

/** An event produced when the controller presses or releases a key on the keyboard.
 *
 *  In most cases, you should use the `keyString` to identify the logical
 *  meaning of the key, taking into account the user's keyboard layout.
 *
 *  If you want to interpret keys according to their physical location on the
 *  keyboard, irrespective of the configured layout, use `physicalKey`.
 *
 *  @param physicalKey
 *    The physical key, as located on a QWERTY keyboard.
 *  @param keyString
 *    The logical string associate with the key, when taking the keyboard layout into account.
 *  @param repeat
 *    For a `keyDown` event, whether this an automatic repeated event triggered
 *    by the OS because the user kept the key pressed.
 */
class KeyEvent(
  val physicalKey: PhysicalKey,
  val keyString: String,
  val repeat: Boolean,
  val shiftDown: Boolean,
  val controlDown: Boolean,
  val altDown: Boolean,
  val metaDown: Boolean,
):
  def hasAnyControlKey: Boolean =
    shiftDown || controlDown || altDown || metaDown
end KeyEvent
