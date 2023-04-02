package com.funlabyrinthe.core.reflect

trait InspectedData {
  val name: String
  val tpe: InspectedType

  def value: Any

  def valueString: String = value.toString

  final def isReadOnly: Boolean = !this.isInstanceOf[WritableInspectedData]
  final def asWritable: WritableInspectedData = this.asInstanceOf[WritableInspectedData]
}
