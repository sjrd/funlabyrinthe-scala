package com.funlabyrinthe.editor.inspector

import scala.reflect.runtime.universe._

trait ReflectedData extends InspectedData {
  val instanceTpe: Type
  val getter: MethodMirror

  override val name: String = getter.symbol.name.decoded
  override val tpe: Type = {
    getter.symbol.typeSignatureIn(instanceTpe) match {
      case NullaryMethodType(resultType) => resultType
    }
  }

  override def value: Any = {
    getter()
  }
}
