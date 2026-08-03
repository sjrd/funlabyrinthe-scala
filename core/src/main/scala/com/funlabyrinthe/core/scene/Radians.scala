package com.funlabyrinthe.core.scene

import scala.annotation.targetName

object Radians {
  @inline def Pi: Radians = Radians(Math.PI)
  @inline def HalfPi: Radians = Radians(Math.PI / 2.0)
  @inline def Tau: Radians = Radians(Math.TAU)
  @inline def Zero: Radians = Radians(0.0)

  /** Converts degrees to radians, allowing negative angles if input is negative. */
  @inline def fromDegrees(degrees: Double): Radians =
    Radians(Math.toRadians(degrees))
}

final class Radians(private val r: Double) extends AnyVal {
  @inline def +(other: Radians): Radians =
    Radians(r + other.r)

  @inline def -(other: Radians): Radians =
    Radians(r - other.r)

  @inline def *(other: Double): Radians =
    Radians(r * other)

  @inline def /(other: Double): Radians =
    Radians(r / other)

  @targetName("divRadians")
  @inline def /(other: Radians): Double =
    r / other.r

  @inline def unary_- : Radians =
    Radians(-r)

  @inline def toDouble: Double =
    r

  @inline def toDegrees: Double =
    Math.toDegrees(r)
}
