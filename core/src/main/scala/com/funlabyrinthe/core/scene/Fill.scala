package com.funlabyrinthe.core.scene

enum Fill:
  case Color(color: RGBA)

object Fill:
  val None: Color = Color(RGBA.Transparent)
