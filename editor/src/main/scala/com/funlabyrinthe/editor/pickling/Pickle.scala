package com.funlabyrinthe.editor.pickling

sealed trait Pickle
sealed trait NumberPickle extends Pickle
sealed trait IntegerPickle extends NumberPickle

case object NullPickle extends Pickle
case object UnitPickle extends Pickle
case class BooleanPickle(value: Boolean) extends Pickle
case class CharPickle(value: Char) extends Pickle
case class BytePickle(value: Byte) extends IntegerPickle
case class ShortPickle(value: Short) extends IntegerPickle
case class IntPickle(value: Int) extends IntegerPickle
case class LongPickle(value: Long) extends IntegerPickle
case class FloatPickle(value: Float) extends NumberPickle
case class DoublePickle(value: Double) extends NumberPickle
case class StringPickle(value: String) extends Pickle
case class ListPickle(elems: List[Pickle]) extends Pickle
case class ObjectPickle(fields: List[(String, Pickle)]) extends Pickle
case class ByteArrayPickle(value: Array[Byte]) extends Pickle

object IntegerPickle {
  def unapply(pickle: IntegerPickle): Some[Long] = Some(pickle match {
    case BytePickle(v) => v
    case ShortPickle(v) => v
    case IntPickle(v) => v
    case LongPickle(v) => v
  })
}

object NumberPickle {
  def unapply(pickle: NumberPickle): Some[Double] = Some(pickle match {
    case FloatPickle(v) => v
    case DoublePickle(v) => v
    case IntegerPickle(v) => v
  })
}
