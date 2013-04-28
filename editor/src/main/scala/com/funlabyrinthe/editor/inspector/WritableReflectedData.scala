package com.funlabyrinthe.editor.inspector

import scala.reflect.runtime.universe._

trait WritableReflectedData extends ReflectedData {
  val setter: MethodMirror

  override val isReadOnly = false

  override def value_=(v: Any) {
    val m = runtimeMirror(v.getClass.getClassLoader)
    val valueTpe = m.reflect(v).symbol.toType
    require(
        (definitions.ScalaPrimitiveValueClasses contains tpe.typeSymbol) ||
        (valueTpe <:< this.tpe),
        s"Cannot assign value $v of type $valueTpe to property of type $tpe")

    setter(v)
  }
}
