package com.funlabyrinthe.core.input

/** Constants for well-known special values of `KeyEvent.keyString`. */
object KeyStrings:
  // Modifier keys
  inline val Alt = "Alt"
  inline val AltGraph = "AltGraph"
  inline val Control = "Control"
  inline val Meta = "Meta"
  inline val Shift = "Shift"

  // Whitespace keys
  inline val Enter = "Enter"
  inline val Tab = "Tab"
  inline val Space = "Space"

  // Navigation keys
  inline val ArrowDown = "ArrowDown"
  inline val ArrowLeft = "ArrowLeft"
  inline val ArrowRight = "ArrowRight"
  inline val ArrowUp = "ArrowUp"
  inline val End = "End"
  inline val Home = "Home"
  inline val PageDown = "PageDown"
  inline val PageUp = "PageUp"

  // UI keys
  inline val ContextMenu = "ContextMenu"
  inline val Escape = "Escape"

  // IME and composition keys
  inline val Dead = "Dead"
end KeyStrings
