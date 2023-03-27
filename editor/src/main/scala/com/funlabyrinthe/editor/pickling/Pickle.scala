package com.funlabyrinthe.editor.pickling

sealed trait Pickle {
  override def toString(): String =
    show(indent = "")

  private def show(indent: String): String = this match {
    case NullPickle =>
      "null"
    case UnitPickle =>
      "undefined"
    case BooleanPickle(value) =>
      value.toString()
    case CharPickle(value) =>
      s"'$value'"
    case IntegerPickle(value) =>
      value.toString()
    case NumberPickle(value) =>
      value.toString()
    case StringPickle(value) =>
      "\"" + value + "\""
    case ListPickle(elems) =>
      if (elems.isEmpty) {
        "[]"
      } else {
        val nestedIndent = indent + "  "
        elems.map(_.show(nestedIndent))
          .mkString(s"[\n$nestedIndent", s",\n$nestedIndent", s",\n$indent]")
      }
    case ObjectPickle(fields) =>
      if (fields.isEmpty) {
        "{}"
      } else {
        val nestedIndent = indent + "  "
        fields.map(f => s"""\"${f._1}\": ${f._2.show(nestedIndent)}""")
          .mkString(s"{\n$nestedIndent", s",\n$nestedIndent", s",\n$indent}")
      }
    case ByteArrayPickle(value) =>
      s"<byte-array len=${value.length}>"
  }
}

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
