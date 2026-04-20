package com.funlabyrinthe.editor.renderer.scene

import scala.reflect.ClassTag

import upickle.default.{*, given}

object SceneSerializers {
  given IArrayReaderWriter[A](using ReadWriter[A], ClassTag[A]): ReadWriter[IArray[A]] =
    summon[ReadWriter[Array[A]]].bimap(Array.from(_), IArray.unsafeFromArray(_))

  given ReadWriter[FontKey] = macroRW
  given ReadWriter[Point] = macroRW
  given ReadWriter[Size] = macroRW
  given ReadWriter[Rectangle] = macroRW
  given ReadWriter[Circle] = macroRW
  given ReadWriter[RGBA] = macroRW
  given ReadWriter[Fill.Color] = macroRW
  given ReadWriter[Fill] = macroRW
  given ReadWriter[Stroke] = macroRW
  given ReadWriter[Material] = macroRW
  given ReadWriter[Graphic] = macroRW
  given ReadWriter[Group] = macroRW
  given ShapeBox: ReadWriter[Shape.Box] = macroRW
  given ShapeCircle: ReadWriter[Shape.Circle] = macroRW
  given ShapeLine: ReadWriter[Shape.Line] = macroRW
  given ShapePolygon: ReadWriter[Shape.Polygon] = macroRW
  given ReadWriter[Text] = macroRW
  given ReadWriter[SceneNode] = macroRW
  given ReadWriter[SceneUpdateFragment] = macroRW
}
