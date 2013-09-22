package com.funlabyrinthe.editor.reflect

import scala.reflect.runtime.universe._

class FieldIRData(instance: InstanceMirror, fir: FieldIR) extends InspectedData {
  override val name: String = fir.name
  override val tpe: Type = fir.tpe

  override val isReadOnly = !fir.hasSetter

  override def value: Any = {
    fieldMirror.get
  }

  override def value_=(v: Any): Unit = {
    fieldMirror.set(v)
  }

  private def fieldMirror = instance.reflectField(fir.field.get)
}
