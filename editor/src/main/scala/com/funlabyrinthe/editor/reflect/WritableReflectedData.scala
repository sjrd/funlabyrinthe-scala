package com.funlabyrinthe.editor.reflect

import scala.reflect.runtime.universe._

private[reflect] trait WritableReflectedData extends ReflectedData {
  val setter: MethodMirror

  override val isReadOnly = false

  override def value_=(v: Any) {
    require(tpe.isValue(v),
        s"Cannot assign value $v (of ${v.getClass()}) to property of type $tpe")

    setter(v)
  }
}
