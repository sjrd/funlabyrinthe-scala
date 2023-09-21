package com.funlabyrinthe.core.reflect

import scala.collection.immutable.ListMap

import com.funlabyrinthe.core.pickling.*

trait Reflectable:
  def reflect(): Reflector[? >: this.type]

  final protected def autoReflect[T >: this.type](using reflector: Reflector[T]): Reflector[T] =
    reflector

  private def reflectProperties(): List[InspectedData] =
    reflect().reflectProperties(this)

  def save()(using PicklingContext): ListMap[String, Pickle] =
    val pickledFields =
      for propData <- reflectProperties() if propData.isPickleable yield
        (propData.name, propData.pickle())
    ListMap.from(pickledFields)
  end save

  def load(pickleFields: Map[String, Pickle])(using PicklingContext): Unit =
    for propData <- reflectProperties() do
      if propData.isPickleable then
        pickleFields.get(propData.name).foreach(propData.unpickle(_))
  end load
end Reflectable
