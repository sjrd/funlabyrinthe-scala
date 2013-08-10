package com.funlabyrinthe.core.graphics

case class Insets(top: Double, right: Double, bottom: Double, left: Double)

object Insets extends ((Double, Double, Double, Double) => Insets) {
  val Empty: Insets = new Insets(0, 0, 0, 0)
}
