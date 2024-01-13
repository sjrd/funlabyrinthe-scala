package com.funlabyrinthe.core.pickling

import scala.scalajs.js
import scala.scalajs.js.JSConverters.*

object JSONPickle:
  def pickleToJSON(pickle: Pickle): Any =
    pickle match
      case NullPickle           => null
      case BooleanPickle(value) => value
      case IntegerPickle(value) => js.Dynamic.literal(integerValue = value)
      case DecimalPickle(value) => value.toDouble
      case StringPickle(value)  => value
      case ListPickle(elems)    => elems.map(pickleToJSON).toJSArray
      case ObjectPickle(fields) => js.special.objectLiteral(fields.map((k, v) => (k, pickleToJSON(v)))*)
  end pickleToJSON

  def jsonToPickle(json: Any): Pickle =
    json match
      case null               => NullPickle
      case value: Boolean     => BooleanPickle(value)
      case value: Double      => DecimalPickle(value)
      case value: String      => StringPickle(value)
      case value: js.Array[?] => ListPickle(value.toList.map(jsonToPickle(_)))

      case value: js.Object =>
        val dict = value.asInstanceOf[js.Dictionary[Any]]
        if dict.contains("integerValue") then
          IntegerPickle(dict("integerValue").asInstanceOf[String])
        else
          ObjectPickle(dict.toList.map((k, v) => (k, jsonToPickle(v))))
  end jsonToPickle
end JSONPickle
