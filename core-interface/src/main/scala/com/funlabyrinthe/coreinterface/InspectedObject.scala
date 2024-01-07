package com.funlabyrinthe.coreinterface

import scala.scalajs.js

trait InspectedObject extends js.Object:
  import InspectedObject.*

  val properties: js.Array[InspectedProperty]
end InspectedObject

object InspectedObject:
  trait InspectedProperty extends js.Object:
    val name: String
    val stringRepr: String
    val editor: PropertyEditor
    val setStringRepr: js.Function1[String, Unit]
  end InspectedProperty

  opaque type PropertyEditorKind = String

  object PropertyEditorKind:
    val String: PropertyEditorKind = "string"
    val Boolean: PropertyEditorKind = "boolean"
    val Int: PropertyEditorKind = "int"
    val StringChoices: PropertyEditorKind = "stringchoices"
    val Painter: PropertyEditorKind = "painter"
    val FiniteSet: PropertyEditorKind = "finiteset"
  end PropertyEditorKind

  trait PropertyEditor extends js.Object:
    val kind: PropertyEditorKind
  end PropertyEditor

  object PropertyEditor:
    object StringValue:
      def apply(): PropertyEditor =
        new PropertyEditor {
          val kind = PropertyEditorKind.String
        }

      def unapply(propEditor: PropertyEditor): Boolean =
        propEditor.kind == PropertyEditorKind.String
    end StringValue

    object BooleanValue:
      def apply(): PropertyEditor =
        new PropertyEditor {
          val kind = PropertyEditorKind.Boolean
        }

      def unapply(propEditor: PropertyEditor): Boolean =
        propEditor.kind == PropertyEditorKind.Boolean
    end BooleanValue

    object IntValue:
      def apply(): PropertyEditor =
        new PropertyEditor {
          val kind = PropertyEditorKind.Int
        }

      def unapply(propEditor: PropertyEditor): Boolean =
        propEditor.kind == PropertyEditorKind.Int
    end IntValue

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

    object PainterValue:
      def apply(): PropertyEditor =
        new PropertyEditor {
          val kind = PropertyEditorKind.Painter
        }

      def unapply(propEditor: PropertyEditor): Boolean =
        propEditor.kind == PropertyEditorKind.Painter
    end PainterValue

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

  trait FiniteSetPropertyEditor extends PropertyEditor:
    val availableElements: js.Array[String]
  end FiniteSetPropertyEditor
end InspectedObject
