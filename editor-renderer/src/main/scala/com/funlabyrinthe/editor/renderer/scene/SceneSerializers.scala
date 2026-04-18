package com.funlabyrinthe.editor.renderer.scene

import scala.reflect.ClassTag

import upickle.default.{*, given}

object SceneSerializers {
  given IArrayReaderWriter[A](using ReadWriter[A], ClassTag[A]): ReadWriter[IArray[A]] =
    summon[ReadWriter[Vector[A]]].bimap(_.toVector, IArray.from(_))
    
  given ReadWriter[Point] = macroRW
  given ReadWriter[Size] = macroRW
  given ReadWriter[Rectangle] = macroRW
  given ReadWriter[RGBA] = macroRW
  given ReadWriter[Material] = macroRW
  given ReadWriter[Graphic] = macroRW
  given ReadWriter[Group] = macroRW
  given ReadWriter[SceneNode] = macroRW
  given ReadWriter[SceneUpdateFragment] = macroRW
}
