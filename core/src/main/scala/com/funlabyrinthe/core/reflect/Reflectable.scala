package com.funlabyrinthe.core.reflect

import scala.collection.immutable.ListMap

import com.funlabyrinthe.core.pickling.*

trait Reflectable:
  def reflect(): Reflector[? >: this.type]

  final protected def autoReflect[T >: this.type](using reflector: Reflector[T]): Reflector[T] =
    reflector

  protected def reflectProperties(): List[InspectedData] =
    reflect().reflectProperties(this)

  private def save()(using PicklingContext): ListMap[String, Pickle] =
    val pickledFields =
      for propData <- reflectProperties() if propData.isPickleable yield
        (propData.name, propData.pickle())
    ListMap.from(pickledFields)
  end save

  private def load(pickleFields: Map[String, Pickle])(using PicklingContext): Unit =
    for propData <- reflectProperties() do
      if propData.isPickleable then
        pickleFields.get(propData.name).foreach(propData.unpickle(_))
  end load
end Reflectable

object Reflectable:
  given ReflectablePickleable: InPlacePickleable[Reflectable] with
    def pickle(value: Reflectable)(using PicklingContext): Pickle =
      ObjectPickle(value.save().toList)
    end pickle

    def unpickle(value: Reflectable, pickle: Pickle)(using PicklingContext): Unit =
      pickle match {
        case ObjectPickle(fields) =>
          value.load(fields.toMap)
        case _ =>
          ()
      }
    end unpickle
  end ReflectablePickleable
end Reflectable
