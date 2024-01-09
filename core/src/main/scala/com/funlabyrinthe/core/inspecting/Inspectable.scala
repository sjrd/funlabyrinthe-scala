package com.funlabyrinthe.core.inspecting

import scala.deriving.*
import scala.collection.immutable.TreeSet
import scala.compiletime.{erasedValue, summonInline}
import scala.reflect.TypeTest

import com.funlabyrinthe.core.*
import com.funlabyrinthe.core.graphics.Painter

trait Inspectable[V]:
  def display(value: V)(using Universe): String = value.toString()

  type EditorValueType

  def editor(using Universe): Editor { type ValueType = EditorValueType }
  def toEditorValue(value: V)(using Universe): EditorValueType
  def fromEditorValue(editorValue: EditorValueType)(using Universe): V
end Inspectable

object Inspectable:
  trait StringChoices[V] extends Inspectable[V]:
    type EditorValueType = String

    def choices(using Universe): List[V]
  end StringChoices

  inline def derived[V](using m: Mirror.SumOf[V])(using singletons: AllSingletons[m.MirroredElemTypes]): StringChoices[V] =
    val choices0: List[V] = singletons.values.asInstanceOf[List[V]]

    new StringChoices[V] {
      def choices(using Universe): List[V] = choices0

      def editor(using Universe): Editor.StringChoices = Editor.StringChoices(choices.map(_.toString()))
      def toEditorValue(value: V)(using Universe): EditorValueType = value.toString()
      def fromEditorValue(editorValue: EditorValueType)(using Universe): V =
        choices.find(_.toString() == editorValue).getOrElse {
          throw IllegalArgumentException(s"Invalid value: '$editorValue'")
        }
    }
  end derived

  sealed trait AllSingletons[T <: Tuple]:
    def values: List[Any]
  end AllSingletons

  object AllSingletons:
    given Empty: AllSingletons[EmptyTuple] with
      def values: List[Any] = Nil

    given Cons[H, R <: Tuple](using h: Mirror.ProductOf[H] { type MirroredElemTypes = EmptyTuple }, r: AllSingletons[R]): AllSingletons[H *: R] with
      def values: List[Any] = h.fromProduct(EmptyTuple) :: r.values
  end AllSingletons

  given StringIsInspectable: Inspectable[String] with
    type EditorValueType = String

    def editor(using Universe): Editor.Text.type = Editor.Text
    def toEditorValue(value: String)(using Universe): EditorValueType = value
    def fromEditorValue(editorValue: EditorValueType)(using Universe): String = editorValue
  end StringIsInspectable

  given BooleanIsInspectable: Inspectable[Boolean] with
    type EditorValueType = Boolean

    def editor(using Universe): Editor.Switch.type = Editor.Switch
    def toEditorValue(value: Boolean)(using Universe): EditorValueType = value
    def fromEditorValue(editorValue: EditorValueType)(using Universe): Boolean = editorValue
  end BooleanIsInspectable

  given ByteIsInspectable: Inspectable[Byte] with
    type EditorValueType = Int

    def editor(using Universe): Editor.SmallInteger = Editor.Int8
    def toEditorValue(value: Byte)(using Universe): EditorValueType = value.toInt
    def fromEditorValue(editorValue: EditorValueType)(using Universe): Byte = editorValue.toByte
  end ByteIsInspectable

  given ShortIsInspectable: Inspectable[Short] with
    type EditorValueType = Int

    def editor(using Universe): Editor.SmallInteger = Editor.Int16
    def toEditorValue(value: Short)(using Universe): EditorValueType = value.toInt
    def fromEditorValue(editorValue: EditorValueType)(using Universe): Short = editorValue.toShort
  end ShortIsInspectable

  given IntIsInspectable: Inspectable[Int] with
    type EditorValueType = Int

    def editor(using Universe): Editor.SmallInteger = Editor.Int32
    def toEditorValue(value: Int)(using Universe): EditorValueType = value
    def fromEditorValue(editorValue: EditorValueType)(using Universe): Int = editorValue
  end IntIsInspectable

  given PainterIsInspectable: Inspectable[Painter] with
    type EditorValueType = List[Painter.PainterItem]

    override def display(value: Painter)(using Universe): String =
      if value.items.isEmpty then "(empty)"
      else "(painter)"

    def editor(using Universe): Editor.Painter.type = Editor.Painter
    def toEditorValue(value: Painter)(using Universe): EditorValueType = value.items

    def fromEditorValue(editorValue: EditorValueType)(using Universe): Painter =
      summon[Universe].EmptyPainter ++ editorValue
  end PainterIsInspectable

  given ComponentRefIsInspectable[V <: Component](using TypeTest[Component, V]): StringChoices[V] with
    def choices(using Universe): List[V] =
      summon[Universe].components[V].sortBy(_.id).toList

    def editor(using Universe): Editor.StringChoices =
      Editor.StringChoices(choices.map(_.id))

    def toEditorValue(value: V)(using Universe): EditorValueType = value.id

    def fromEditorValue(editorValue: String)(using Universe): V =
      summon[Universe].getComponentByID(editorValue) match
        case c: V => c
        case c    => throw IllegalArgumentException(s"Illegal component '$c'")
    end fromEditorValue
  end ComponentRefIsInspectable

  given TreeSetOfStringChoicesIsInspectable[V](using StringChoices[V], Ordering[V]): Inspectable[TreeSet[V]] with
    type EditorValueType = List[String]

    private def stringChoices: StringChoices[V] = summon[StringChoices[V]]

    def choices(using Universe): List[V] =
      stringChoices.choices

    def editor(using Universe): Editor.MultiStringChoices =
      Editor.MultiStringChoices(choices.map(_.toString()))

    def toEditorValue(value: TreeSet[V])(using Universe): EditorValueType =
      value.toList.map(stringChoices.toEditorValue(_))

    def fromEditorValue(editorValue: List[String])(using Universe): TreeSet[V] =
      TreeSet.from(editorValue.map(stringChoices.fromEditorValue(_)))
  end TreeSetOfStringChoicesIsInspectable
end Inspectable
