package com.funlabyrinthe.editor.renderer.inspector

class InspectedObject(val properties: List[InspectedObject.InspectedProperty])

object InspectedObject:
  class InspectedProperty(val name: String, val stringRepr: String, val editor: PropertyEditor)

  enum PropertyEditor:
    case StringValue
    case BooleanValue
end InspectedObject
