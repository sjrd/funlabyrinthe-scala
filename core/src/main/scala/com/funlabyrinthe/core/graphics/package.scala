package com.funlabyrinthe.core

package object graphics {

  def fillWithOpaqueBackground(context: DrawContext) {
    import context._
    val SmallSquareSize = 16.0

    gc.save()

    val evenFill = Color.Black
    val oddFill = Color.DimGray

    val xrange = rect.minX until rect.maxX by SmallSquareSize
    val yrange = rect.minY until rect.maxY by SmallSquareSize

    for (i <- 0 until xrange.length; j <- 0 until yrange.length) {
      gc.fill = if ((i+j) % 2 == 0) evenFill else oddFill
      gc.fillRect(xrange(i), yrange(j), SmallSquareSize, SmallSquareSize)
    }

    gc.restore()
  }
}
