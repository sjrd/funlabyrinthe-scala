package com.funlabyrinthe.core.reflect

import com.funlabyrinthe.core.pickling.*

trait InspectedData {
  val name: String
  val tpe: InspectedType

  type Value

  def value: Value

  def valueString: String = value.toString

  final def isReadOnly: Boolean = !this.isInstanceOf[WritableInspectedData]
  final def asWritable: WritableInspectedData = this.asInstanceOf[WritableInspectedData]

  def isPickleable: Boolean

  def pickle()(using PicklingContext): Pickle

  def unpickle(pickle: Pickle)(using PicklingContext): Unit
}
