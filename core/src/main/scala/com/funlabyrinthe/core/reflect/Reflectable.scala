package com.funlabyrinthe.core.reflect

import scala.collection.immutable.ListMap

import com.funlabyrinthe.core.pickling.*

trait Reflectable:
  import Reflectable.*

  def reflect(): Reflector[? >: this.type]

  final protected def autoReflect[T >: this.type](using reflector: Reflector[T]): Reflector[T] =
    reflector

  def save()(using Context): ListMap[String, Pickle] =
    val pickledFields = for {
      (propData, propPickler) <- reflectingPicklersForProperties(this)
    } yield {
      (propData.name, propPickler.pickle(propData))
    }

    ListMap.from(pickledFields)
  end save

  def load(pickleFields: Map[String, Pickle])(using Context): Unit =
    for {
      (propData, propPickler) <- reflectingPicklersForProperties(this)
    } {
      pickleFields.get(propData.name) foreach { propPickle =>
        propPickler.unpickle(propData, propPickle)
      }
    }
  end load
end Reflectable

object Reflectable:
  /** Enumerate the reflected data for properties of an instance. */
  private def reflectingPicklersForProperties(value: Reflectable)(
      using Context): List[(InspectedData, Pickler)] =

    for
      data <- value.reflect().reflectProperties(value)
      pickler <- summon[Context].registry.createPickler(data)
    yield
      (data, pickler)
  end reflectingPicklersForProperties
end Reflectable
