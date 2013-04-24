package com.funlabyrinthe.editor.inspector

import scala.reflect.runtime.{ universe => ru }

abstract class PropertyDescriptor(val mirror: ru.MethodMirror) {
  def name: String = mirror.symbol.name.decoded
  def valueString: String = mirror.apply().toString
}

class ReadOnlyPropertyDescriptor(m: ru.MethodMirror) extends PropertyDescriptor(m) {
}
