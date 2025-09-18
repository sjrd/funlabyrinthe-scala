package com.funlabyrinthe.coreinterface

import scala.reflect.Typeable

import scala.scalajs.js
import scala.scalajs.js.JSConverters.*

trait InspectedObject extends js.Object:
  import InspectedObject.*

  val properties: js.Array[InspectedProperty]
end InspectedObject

object InspectedObject:
  trait InspectedProperty extends js.Object:
    val name: String
    val valueDisplayString: String
    val editor: PropertyEditor
    val serializedEditorValue: Any
    val setSerializedEditorValue: js.Function1[Any, Unit]
  end InspectedProperty

  opaque type PropertyEditorKind = String

  object PropertyEditorKind:
    val String: PropertyEditorKind = "string"
    val Boolean: PropertyEditorKind = "boolean"
    val Int: PropertyEditorKind = "int"
    val StringChoices: PropertyEditorKind = "stringchoices"
    val ItemList: PropertyEditorKind = "itemlist"
    val Painter: PropertyEditorKind = "painter"
    val FiniteSet: PropertyEditorKind = "finiteset"
    val Color: PropertyEditorKind = "color"
  end PropertyEditorKind

  trait PropertyEditor extends js.Object:
    val kind: PropertyEditorKind
  end PropertyEditor

  object PropertyEditor:
    /** Free-form text editor. The associated serialized type is a `String`. */
    object StringValue:
      def apply(): PropertyEditor =
        new PropertyEditor {
          val kind = PropertyEditorKind.String
        }

      def unapply(propEditor: PropertyEditor): Boolean =
        propEditor.kind == PropertyEditorKind.String
    end StringValue

    /** Switch. The associated serialized type is a `Boolean`. */
    object BooleanValue:
      def apply(): PropertyEditor =
        new PropertyEditor {
          val kind = PropertyEditorKind.Boolean
        }

      def unapply(propEditor: PropertyEditor): Boolean =
        propEditor.kind == PropertyEditorKind.Boolean
    end BooleanValue

    /** Small integer. The associated serialized type is an `Int`. */
    object IntValue:
      def apply(): PropertyEditor =
        new PropertyEditor {
          val kind = PropertyEditorKind.Int
        }

      def unapply(propEditor: PropertyEditor): Boolean =
        propEditor.kind == PropertyEditorKind.Int
    end IntValue

    /** Choice between multiple (distinct) strings. The associated serialized type is a `String`. */
    object StringChoices:
      def apply(choices: js.Array[String]): PropertyEditor =
        val choices0 = choices
        new StringChoicesPropertyEditor {
          val kind = PropertyEditorKind.StringChoices
          val choices = choices0
        }

      def unapply(propEditor: PropertyEditor): Option[js.Array[String]] =
        if propEditor.kind == PropertyEditorKind.StringChoices then
          Some(propEditor.asInstanceOf[StringChoicesPropertyEditor].choices)
        else
          None
    end StringChoices

    /** Editable list of items with a sub editor.
     *
     *  The associated serialized type is a `js.Array[E]` where `E` is the
     *  associated serialized type of the `elemEditor`.
     */
    object ItemList:
      def apply(elemEditor: PropertyEditor): PropertyEditor =
        val elemEditor0 = elemEditor
        new ItemListPropertyEditor {
          val kind = PropertyEditorKind.ItemList
          val elemEditor = elemEditor0
        }

      def unapply(propEditor: PropertyEditor): Option[PropertyEditor] =
        if propEditor.kind == PropertyEditorKind.ItemList then
          Some(propEditor.asInstanceOf[ItemListPropertyEditor].elemEditor)
        else
          None
    end ItemList

    /** Painter. The associated serialized type is a `js.Array[PainterItem]`. */
    object PainterValue:
      def apply(): PropertyEditor =
        new PropertyEditor {
          val kind = PropertyEditorKind.Painter
        }

      def unapply(propEditor: PropertyEditor): Boolean =
        propEditor.kind == PropertyEditorKind.Painter

      trait PainterItem extends js.Object:
        val name: String
      end PainterItem
    end PainterValue

    /** Color. The associated serialized type is a packed `Int` in word-order. */
    object ColorValue:
      def apply(): PropertyEditor =
        new PropertyEditor {
          val kind = PropertyEditorKind.Color
        }

      def unapply(propEditor: PropertyEditor): Boolean =
        propEditor.kind == PropertyEditorKind.Color
    end ColorValue

    /** Multiple choice of (distinct) strings. The associated serialized type is a `js.Array[String]`. */
    object FiniteSet:
      def apply(availableElements: js.Array[String]): PropertyEditor =
        val availableElements0 = availableElements
        new FiniteSetPropertyEditor {
          val kind = PropertyEditorKind.FiniteSet
          val availableElements = availableElements0
        }

      def unapply(propEditor: PropertyEditor): Option[js.Array[String]] =
        if propEditor.kind == PropertyEditorKind.FiniteSet then
          Some(propEditor.asInstanceOf[FiniteSetPropertyEditor].availableElements)
        else
          None
    end FiniteSet
  end PropertyEditor

  trait StringChoicesPropertyEditor extends PropertyEditor:
    val choices: js.Array[String]
  end StringChoicesPropertyEditor

  trait ItemListPropertyEditor extends PropertyEditor:
    val elemEditor: PropertyEditor
  end ItemListPropertyEditor

  trait FiniteSetPropertyEditor extends PropertyEditor:
    val availableElements: js.Array[String]
  end FiniteSetPropertyEditor

  trait Serializer[T]:
    def serialize(value: T): Any
    def deserialize(serializedValue: Any): T

    protected def illegalSerializedValue(serializedValue: Any): Nothing =
      throw IllegalArgumentException(
        s"Illegal serialized value for $this: $serializedValue (${serializedValue.getClass()})"
      )

    override def toString(): String = this.getClass().getSimpleName()
  end Serializer

  object Serializer:
    abstract class SameTypeSerializer[T](using Typeable[T]) extends Serializer[T]:
      def serialize(value: T): Any = value

      def deserialize(serializedValue: Any): T = serializedValue match
        case value: T => value
        case _        => illegalSerializedValue(serializedValue)
    end SameTypeSerializer

    given StringSerializer: SameTypeSerializer[String] with {}
    given IntSerializer: SameTypeSerializer[Int] with {}
    given BooleanSerializer: SameTypeSerializer[Boolean] with {}

    given ListSerializer[E](using elemSerializer: Serializer[E]): Serializer[List[E]] with
      def serialize(value: List[E]): Any =
        value.map(elemSerializer.serialize(_)).toJSArray

      def deserialize(serializedValue: Any): List[E] = serializedValue match
        case serializedValue: js.Array[?] => serializedValue.toList.map(elemSerializer.deserialize(_))
        case _                            => illegalSerializedValue(serializedValue)
    end ListSerializer
  end Serializer
end InspectedObject
