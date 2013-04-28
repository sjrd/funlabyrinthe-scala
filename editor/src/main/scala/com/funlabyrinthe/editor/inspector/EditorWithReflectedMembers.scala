package com.funlabyrinthe.editor.inspector

import scala.reflect.runtime.universe._

trait EditorWithReflectedMembers extends EditorWithLazyMembers {
  require(data.tpe <:< definitions.AnyRefTpe,
      s"EditorWithReflectedMembers requires an AnyRef type, found ${data.tpe}")

  override def complete() {
    import ReflectionUtils._
    val instanceMirror = reflectInstance(data.value)
    val tpe = guessRuntimeTypeOf(instanceMirror, data.tpe)
    childEditors ++= reflectingEditorsForProperties(
        inspector, instanceMirror, tpe)
  }
}
