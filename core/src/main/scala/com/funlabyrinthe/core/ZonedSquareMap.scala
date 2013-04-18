package com.funlabyrinthe.core

trait ZonedSquareMap extends SquareMap {
  val ZoneWidth = 7
  val ZoneHeight = 7
  final def ZoneSize = (ZoneWidth, ZoneHeight)
}
