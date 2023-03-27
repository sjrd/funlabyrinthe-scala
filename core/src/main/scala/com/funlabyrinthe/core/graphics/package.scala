package com.funlabyrinthe.core

package object graphics {

  def fillWithOpaqueBackground(context: DrawContext): Unit = {
    import context._
    val SmallSquareSize = 16.0

    gc.save()

    val evenFill = Color.Black
    val oddFill = Color.DimGray

    var x = rect.minX
    var i = 0
    while (x < rect.maxX) {
      var y = rect.minY
      var j = 0
      while (y < rect.maxY) {
        gc.fill = if ((i+j) % 2 == 0) evenFill else oddFill
        gc.fillRect(x, y, SmallSquareSize, SmallSquareSize)

        y += SmallSquareSize
        j += 1
      }
      x += SmallSquareSize
      i += 1
    }

    /* Does not work with Scala.js
    val xrange = rect.minX until rect.maxX by SmallSquareSize
    val yrange = rect.minY until rect.maxY by SmallSquareSize

    for (i <- 0 until xrange.length; j <- 0 until yrange.length) {
      gc.fill = if ((i+j) % 2 == 0) evenFill else oddFill
      gc.fillRect(xrange(i), yrange(j), SmallSquareSize, SmallSquareSize)
    }
    */

    gc.restore()
  }
}
