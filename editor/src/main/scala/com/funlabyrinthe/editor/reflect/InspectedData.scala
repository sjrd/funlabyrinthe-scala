package com.funlabyrinthe.editor.reflect

import scala.reflect.runtime.universe._

trait InspectedData {
  val name: String
  val tpe: Type
  val isReadOnly: Boolean = true

  def value: Any
  def value_=(v: Any): Unit

  def valueString: String = value.toString
}
