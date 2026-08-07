package com.funlabyrinthe.scene

final case class RGBA(red: Double, green: Double, blue: Double, alpha: Double)

object RGBA {
  val White = RGBA(1, 1, 1, 1)
}
