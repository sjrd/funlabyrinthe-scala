package com.funlabyrinthe.core

trait ZonedSquareMap extends SquareMap {
  val zoneWidth = 7
  val zoneHeight = 7
  final def zoneSize = (zoneWidth, zoneHeight)
}
