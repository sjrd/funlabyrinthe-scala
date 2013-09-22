package com.funlabyrinthe.editor.reflect

import scala.reflect.runtime.universe._

class ReadOnlyReflectedData(val instanceTpe: Type,
    val getter: MethodMirror) extends ReflectedData {

  def value_=(v: Any) {
    throw new UnsupportedOperationException(
        s"Property ${this.name} is readonly")
  }
}
