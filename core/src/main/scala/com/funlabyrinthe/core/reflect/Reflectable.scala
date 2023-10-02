package com.funlabyrinthe.core.reflect

import scala.collection.immutable.ListMap

import com.funlabyrinthe.core.pickling.*

trait Reflectable:
  def reflect(): Reflector[? >: this.type]

  final protected def autoReflect[T >: this.type](using reflector: Reflector[T]): Reflector[T] =
    reflector

  protected def reflectProperties(): List[InspectedData] =
    reflect().reflectProperties(this)

  // Basically a `lazy val` but with less binary compatibility footprint
  private var _reflectedProperties: List[InspectedData] | Null = null

  private def reflectedProperties: List[InspectedData] =
    val local = _reflectedProperties
    if local != null then
      local
    else
      val computed = reflectProperties()
      _reflectedProperties = computed
      computed
  end reflectedProperties

  private def storeDefaults(): Unit =
    for propData <- reflectedProperties if propData.isPickleable do
      propData.storeDefaults()
  end storeDefaults

  private def save()(using PicklingContext): List[(String, Pickle)] =
    for
      propData <- reflectedProperties
      if propData.isPickleable
      pickle <- propData.pickle()
    yield
      (propData.name, pickle)
  end save

  private def load(pickleFields: Map[String, Pickle])(using PicklingContext): Unit =
    for propData <- reflectedProperties do
      if propData.isPickleable then
        pickleFields.get(propData.name).foreach(propData.unpickle(_))
  end load
end Reflectable

object Reflectable:
  given ReflectablePickleable: InPlacePickleable[Reflectable] with
    def storeDefaults(value: Reflectable): Unit =
      value.storeDefaults()

    def pickle(value: Reflectable)(using PicklingContext): Option[Pickle] =
      val fields = value.save()
      if fields.isEmpty then None
      else Some(ObjectPickle(fields))
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
