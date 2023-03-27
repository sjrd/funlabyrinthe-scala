package com.funlabyrinthe.editor.reflect

import scala.reflect.runtime.universe._

private[reflect] final class ReadOnlyReflectedData(
    val instanceTpe: Type, val getter: MethodMirror)
    extends ReflectedData {

  def value_=(v: Any): Unit = {
    throw new UnsupportedOperationException(
        s"Property ${this.name} is readonly")
  }
}
