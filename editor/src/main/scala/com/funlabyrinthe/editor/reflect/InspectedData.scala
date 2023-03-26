package com.funlabyrinthe.editor.reflect

trait InspectedData {
  val name: String
  val tpe: InspectedType
  val isReadOnly: Boolean = true

  def value: Any
  def value_=(v: Any): Unit

  def valueString: String = value.toString
}
