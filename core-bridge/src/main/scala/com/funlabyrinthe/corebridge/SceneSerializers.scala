package com.funlabyrinthe.corebridge

import scala.reflect.ClassTag

import com.funlabyrinthe.core.scene.*

import upickle.default.{*, given}

object SceneSerializers {
  given IArrayReaderWriter[A](using ReadWriter[A], ClassTag[A]): ReadWriter[Batch[A]] =
    summon[ReadWriter[Array[A]]].bimap(_.toIndexedSeq.toArray, a => Batch.from(IArray.unsafeFromArray(a)))

  given ReadWriter[Point] = macroRW
  given ReadWriter[Size] = macroRW
  given ReadWriter[Rectangle] = macroRW
  given ReadWriter[RGBA] = macroRW
  given ReadWriter[Fill.Color] = macroRW
  given ReadWriter[Fill] = macroRW
  given ReadWriter[Stroke] = macroRW
  given ReadWriter[Material] = macroRW
  given ReadWriter[Graphic] = macroRW
  given ReadWriter[Group] = macroRW
  given ReadWriter[Shape.Box] = macroRW
  given ReadWriter[SceneNode] = macroRW
  given ReadWriter[SceneUpdateFragment] = macroRW
}
