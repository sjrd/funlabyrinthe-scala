package com.funlabyrinthe.editor.reflect

import scala.reflect.runtime.universe._

private[reflect] trait ReflectedData extends InspectedData {
  val instanceTpe: Type
  val getter: MethodMirror

  override val name: String = getter.symbol.name.decodedName.toString
  override val tpe: InspectedType = {
    getter.symbol.typeSignatureIn(instanceTpe) match {
      case NullaryMethodType(resultType) => new InspectedType(resultType)
    }
  }

  override def value: Any = {
    getter()
  }
}
