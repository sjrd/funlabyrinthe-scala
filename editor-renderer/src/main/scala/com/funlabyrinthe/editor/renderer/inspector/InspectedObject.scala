package com.funlabyrinthe.editor.renderer.inspector

class InspectedObject(val properties: List[InspectedObject.InspectedProperty])

object InspectedObject:
  final case class PropSetEvent(prop: InspectedProperty, newValue: String)

  class InspectedProperty(
    val name: String,
    val stringRepr: String,
    val editor: PropertyEditor,
    val setStringRepr: String => Unit,
  )

  enum PropertyEditor:
    case StringValue
    case BooleanValue
    case IntValue
    case StringChoices(choices: List[String])
    case PainterEditor
    case FiniteSet(availableElements: List[String])
end InspectedObject
