package com.funlabyrinthe.core.input

class MouseEvent(val x: Double, val y: Double, val button: MouseButton):
  override def toString(): String =
    s"MouseEvent(x = $x, y = $y, $button)"
end MouseEvent
