package com.funlabyrinthe.core.graphics

class DrawContext(val gc: GraphicsContext, val rect: Rectangle2D) {
  final def minX = rect.minX
  final def minY = rect.minY
  final def maxX = rect.maxX
  final def maxY = rect.maxY
  final def width = rect.width
  final def height = rect.height
}
