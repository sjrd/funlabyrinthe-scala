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
    val Struct: PropertyEditorKind = "struct"
    val Sum: PropertyEditorKind = "sum"
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

    /** Struct of fields with respective sub editors.
     *
     *  The associated serialized type is a heterogeneous `js.Array[Es]` where
     *  the `Es` are the respective associated serialized types of the
     *  `fieldEditors`.
     */
    object Struct:
      def apply(fieldNames: List[String], fieldEditors: List[PropertyEditor]): PropertyEditor =
        val fieldNames0 = fieldNames.toJSArray
        val fieldEditors0 = fieldEditors.toJSArray
        new StructPropertyEditor {
          val kind = PropertyEditorKind.Struct
          val fieldNames = fieldNames0
          val fieldEditors = fieldEditors0
        }

      def unapply(propEditor: PropertyEditor): Option[(List[String], List[PropertyEditor])] =
        if propEditor.kind == PropertyEditorKind.Struct then
          val propEditor1 = propEditor.asInstanceOf[StructPropertyEditor]
          Some((propEditor1.fieldNames.toList, propEditor1.fieldEditors.toList))
        else
          None
    end Struct

    /** Struct of fields with respective sub editors.
     *
     *  The associated serialized type is a heterogeneous `js.Array[Es]` where
     *  the `Es` are the respective associated serialized types of the
     *  `fieldEditors`.
     */
    object Sum:
      def apply(altNames: List[String], altEditors: List[PropertyEditor]): PropertyEditor =
        val altNames0 = altNames.toJSArray
        val altEditors0 = altEditors.toJSArray
        new SumPropertyEditor {
          val kind = PropertyEditorKind.Sum
          val altNames = altNames0
          val altEditors = altEditors0
        }

      def unapply(propEditor: PropertyEditor): Option[(List[String], List[PropertyEditor])] =
        if propEditor.kind == PropertyEditorKind.Sum then
          val propEditor1 = propEditor.asInstanceOf[SumPropertyEditor]
          Some((propEditor1.altNames.toList, propEditor1.altEditors.toList))
        else
          None
    end Sum

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

  trait StructPropertyEditor extends PropertyEditor:
    val fieldNames: js.Array[String]
    val fieldEditors: js.Array[PropertyEditor]
  end StructPropertyEditor

  trait SumPropertyEditor extends PropertyEditor:
    val altNames: js.Array[String]
    val altEditors: js.Array[PropertyEditor]
  end SumPropertyEditor

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

    def makeTupleSerializer[Es <: Tuple](fieldSerializers: List[Serializer[?]]): Serializer[Es] =
      new Serializer[Es] {
        def serialize(value: Es): Any =
          fieldSerializers.zip(value.toList).map { (fieldSer, fieldValue) =>
            fieldSer.asInstanceOf[Serializer[Any]].serialize(fieldValue)
          }.toJSArray

        def deserialize(serializedValue: Any): Es =
          val resList = fieldSerializers.zip(serializedValue.asInstanceOf[js.Array[Any]].toList).map { (fieldSer, serializedFieldValue) =>
            fieldSer.deserialize(serializedFieldValue)
          }
          Tuple.fromArray(resList.toArray[Any]).asInstanceOf[Es]
      }
    end makeTupleSerializer

    def makeSumSerializer[A](altNames: List[String], altSerializers: List[Serializer[? <: A]]): Serializer[(String, A)] =
      new Serializer[(String, A)] {
        def serialize(value: (String, A)): Any =
          val (altName, altValue) = value
          val ord = altNames.indexOf(altName)
          if ord < 0 then
            throw IllegalArgumentException(s"Unknown alternative: '$altName'")
          altSerializers(ord) match
            case altSerializer: Serializer[a] =>
              val serializedValue = altSerializer.serialize(downCast[A, a](altValue))
              js.Array(altName, serializedValue)

        def deserialize(serializedValue: Any): (String, A) =
          serializedValue match
            case serializedValue: js.Array[?] if serializedValue.length == 2 =>
              val altName = serializedValue(0)
              val ord = altNames.indexOf(altName)
              if ord < 0 then
                illegalSerializedValue(serializedValue)
              else
                (altName.asInstanceOf[String], altSerializers(ord).deserialize(serializedValue(1)))
            case _ =>
              illegalSerializedValue(serializedValue)

        private def downCast[T, S <: T](x: T): S = x.asInstanceOf[S]
      }
    end makeSumSerializer
  end Serializer
end InspectedObject
