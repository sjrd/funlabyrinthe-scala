package com.funlabyrinthe.gamerunner.scene

import scala.collection.mutable
import scala.reflect.ClassTag

import upickle.default.{*, given}
import upickle.core.Visitor
import upickle.core.ArrVisitor

object SceneSerializers {
  given BatchReadWriter[T](using r: ReadWriter[T], ct: ClassTag[T]): ReadWriter[Batch[T]] with SimpleReader[Batch[T]] with {
    def write0[R](out: Visitor[?, R], v: Batch[T]): R = {
      val seq = v.toIndexedSeq
      val len = seq.size
      val ctx = out.visitArray(len, -1).narrow
      var i = 0
      while (i < len) {
        ctx.visitValue(r.write(ctx.subVisitor, seq(i)), -1)
        i += 1
      }

      ctx.visitEnd(-1)
    }

    override def expectedMsg: String = "expected sequence"

    override def visitArray(length: Int, index: Int): ArrVisitor[Any, Batch[T]] = new ArrVisitor[Any, Batch[T]] {
      val b = mutable.ArrayBuilder.make[T]

      def visitValue(v: Any, index: Int): Unit = {
        b += v.asInstanceOf[T]
      }

      def visitEnd(index: Int): Batch[T] = IArray.unsafeFromArray(b.result())

      def subVisitor: Visitor[?, ?] = implicitly[Reader[T]]
    }
  }

  //given IArrayReaderWriter[A](using ReadWriter[A], ClassTag[A]): ReadWriter[Batch[A]] =
  //  summon[ReadWriter[Array[A]]].bimap(_.toIndexedSeq.toArray, a => Batch.from(IArray.unsafeFromArray(a)))

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
