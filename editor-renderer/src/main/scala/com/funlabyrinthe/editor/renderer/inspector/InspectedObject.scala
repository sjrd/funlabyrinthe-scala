package com.funlabyrinthe.editor.renderer.inspector

import com.funlabyrinthe.editor.renderer.PainterItem

final class InspectedObject(val properties: List[InspectedObject.InspectedProperty[?]])

object InspectedObject:
  final case class PropSetEvent[T](prop: InspectedProperty[T], newValue: T)

  final class InspectedList[T](val elems: List[InspectedProperty[T]])

  class InspectedProperty[T](
    val name: String,
    val valueDisplayString: String,
    val editor: PropertyEditor[T],
    val editorValue: T,
    val setEditorValue: T => Unit,
    val remove: Option[() => Unit],
  )

  enum PropertyEditor[T]:
    case StringValue extends PropertyEditor[String]
    case BooleanValue extends PropertyEditor[Boolean]
    case IntValue extends PropertyEditor[Int]
    case StringChoices(choices: List[String]) extends PropertyEditor[String]
    case ItemList[E](elemEditor: PropertyEditor[E]) extends PropertyEditor[List[E]]
    case PainterEditor extends PropertyEditor[List[PainterItem]]
    case ColorEditor extends PropertyEditor[Int]
    case FiniteSet(availableElements: List[String]) extends PropertyEditor[List[String]]

    def hasChildren: Boolean = this match
      case ItemList(_) => true
      case _           => false
  end PropertyEditor
end InspectedObject
