package com.funlabyrinthe.core.scene

enum Fill {
  case Color(color: RGBA)

  case LinearGradient(
    fromPoint: Point,
    fromColor: RGBA,
    toPoint: Point,
    toColor: RGBA
  )
}

object Fill {
  val None: Color = Color(RGBA.Transparent)
}
