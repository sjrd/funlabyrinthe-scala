package com.funlabyrinthe.editor.renderer.inspector

import com.funlabyrinthe.editor.renderer.PainterItem

class InspectedObject(val properties: List[InspectedObject.InspectedProperty[?]])

object InspectedObject:
  final case class PropSetEvent[T](prop: InspectedProperty[T], newValue: T)

  class InspectedProperty[T](
    val name: String,
    val valueDisplayString: String,
    val editor: PropertyEditor[T],
    val editorValue: T,
    val setEditorValue: T => Unit,
  )

  enum PropertyEditor[T]:
    case StringValue extends PropertyEditor[String]
    case BooleanValue extends PropertyEditor[Boolean]
    case IntValue extends PropertyEditor[Int]
    case StringChoices(choices: List[String]) extends PropertyEditor[String]
    case PainterEditor extends PropertyEditor[List[PainterItem]]
    case ColorEditor extends PropertyEditor[Int]
    case FiniteSet(availableElements: List[String]) extends PropertyEditor[List[String]]
end InspectedObject
