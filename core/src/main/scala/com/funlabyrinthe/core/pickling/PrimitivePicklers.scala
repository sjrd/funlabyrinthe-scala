package com.funlabyrinthe.core.pickling

import com.funlabyrinthe.core.reflect._

object PrimitivePicklers {
  def registerPrimitivePicklers(registry: PicklingRegistry): Unit = {
    registry.registerPickleable[String]()
    registry.registerPickleable[Boolean]()
    registry.registerPickleable[Char]()
    registry.registerPickleable[Byte]()
    registry.registerPickleable[Short]()
    registry.registerPickleable[Int]()
    registry.registerPickleable[Long]()
    registry.registerPickleable[Float]()
    registry.registerPickleable[Double]()
  }
}
