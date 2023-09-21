package com.funlabyrinthe.core.reflect

import com.funlabyrinthe.core.pickling.Pickler

trait InspectedData {
  val name: String
  val tpe: InspectedType

  type Value

  def value: Value

  def valueString: String = value.toString

  final def isReadOnly: Boolean = !this.isInstanceOf[WritableInspectedData]
  final def asWritable: WritableInspectedData = this.asInstanceOf[WritableInspectedData]

  def optPickler: Option[Pickler]
}
