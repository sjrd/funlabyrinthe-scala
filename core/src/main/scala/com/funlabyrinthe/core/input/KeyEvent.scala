package com.funlabyrinthe.core.input

class KeyEvent(val code: KeyCode, val shiftDown: Boolean,
    val controlDown: Boolean, val altDown: Boolean, val metaDown: Boolean) {

  def hasAnyControlKey: Boolean =
    shiftDown || controlDown || altDown || metaDown
}
