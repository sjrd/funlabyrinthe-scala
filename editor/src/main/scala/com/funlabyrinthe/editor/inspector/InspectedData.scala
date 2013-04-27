package com.funlabyrinthe.editor.inspector

import scala.reflect.runtime.universe._

trait InspectedData {
  val name: String
  val tpe: Type

  def value: Any
  def value_=(v: Any): Unit

  def valueString: String = value.toString
}
