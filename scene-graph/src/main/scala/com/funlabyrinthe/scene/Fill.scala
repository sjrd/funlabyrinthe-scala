package com.funlabyrinthe.scene

enum Fill {
  case Color(color: RGBA)

  case LinearGradient(
    fromPoint: Point,
    fromColor: RGBA,
    toPoint: Point,
    toColor: RGBA
  )

  case RadialGradient(
    fromPoint: Point,
    fromColor: RGBA,
    toPoint: Point,
    toColor: RGBA,
  )
}
