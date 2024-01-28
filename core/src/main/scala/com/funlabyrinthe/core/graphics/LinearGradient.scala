package com.funlabyrinthe.core.graphics

final class LinearGradient(
  val startPoint: Point2D,
  val endPoint: Point2D,
  val colorStops: List[(Double, Color)],
) extends Paint
