package com.funlabyrinthe.core.pickling

sealed trait Pickle {
  override def toString(): String =
    show(indent = "")

  private def isSimple: Boolean = this match
    case _: ListPickle | _: ObjectPickle => false
    case _                               => true

  private def show(indent: String): String = this match {
    case NullPickle =>
      "null"
    case BooleanPickle(value) =>
      value.toString()
    case IntegerPickle(value) =>
      value
    case DecimalPickle(value) =>
      value
    case StringPickle(value) =>
      "\"" + value + "\""
    case ListPickle(elems) =>
      if (elems.isEmpty) {
        "[]"
      } else {
        if elems.forall(_.isSimple) then
          elems.map(_.show(indent)).mkString("[ ", ", ", ", ]")
        else
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
  }
}

object Pickle:
  def fromString(source: String): Pickle =
    PickleParser.parse(source)
end Pickle

case object NullPickle extends Pickle
case class BooleanPickle(value: Boolean) extends Pickle

case class IntegerPickle(value: String) extends Pickle:
  def intValue: Int = value.toInt
  def longValue: Long = value.toLong
end IntegerPickle

object IntegerPickle:
  def apply(value: Int): IntegerPickle = IntegerPickle(value.toString())
  def apply(value: Long): IntegerPickle = IntegerPickle(value.toString())
end IntegerPickle

case class DecimalPickle(value: String) extends Pickle:
  def floatValue: Float = value.toFloat
  def doubleValue: Double = value.toDouble
end DecimalPickle

object DecimalPickle:
  def apply(value: Float): DecimalPickle = DecimalPickle(value.toDouble)

  def apply(value: Double): DecimalPickle =
    val str1 = value.toString()
    val str2 =
      if value.isInfinite() || value.isNaN() then str1
      else if value.equals(-0.0) then "-0.0"
      else if str1.contains('.') || str1.contains('e') then str1
      else str1 + ".0"
    DecimalPickle(str2)
  end apply
end DecimalPickle

case class StringPickle(value: String) extends Pickle

case class ListPickle(elems: List[Pickle]) extends Pickle

case class ObjectPickle(fields: List[(String, Pickle)]) extends Pickle:
  private val fieldMap = fields.toMap

  def getField(name: String): Option[Pickle] = fieldMap.get(name)

  override def equals(that: Any): Boolean = that match
    case that: ObjectPickle => this.fieldMap == that.fieldMap
    case _                  => false

  override def hashCode(): Int = fieldMap.##
end ObjectPickle

object ObjectPickle:
  val empty: ObjectPickle = ObjectPickle(Nil)
end ObjectPickle
