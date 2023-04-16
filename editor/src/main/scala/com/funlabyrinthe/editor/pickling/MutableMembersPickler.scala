package com.funlabyrinthe.editor.pickling

import com.funlabyrinthe.core.reflect._
import com.funlabyrinthe.editor.reflect._

trait MutableMembersPickler extends Pickler {
  val tpe: InspectedType

  def pickle(data: InspectedData)(implicit ctx: Context): Pickle = {
    pickle(data, Set.empty)
  }

  protected def pickle(data: InspectedData, exclude: Set[String])(
      implicit ctx: Context): Pickle = {
    val pickledFields = for {
      (propData, propPickler) <- reflectingPicklersForProperties(data.value)
      if !exclude.contains(propData.name)
    } yield {
      (propData.name, propPickler.pickle(propData))
    }

    ObjectPickle(pickledFields)
  }

  def unpickle(data: InspectedData, pickle: Pickle)(
      implicit ctx: Context): Unit = {
    pickle match {
      case ObjectPickle(pickleFields) =>
        val pickleMap = Map(pickleFields:_*)

        for {
          (propData, propPickler) <- reflectingPicklersForProperties(data.value)
        } {
          pickleMap.get(propData.name) foreach { propPickle =>
            propPickler.unpickle(propData, propPickle)
          }
        }

      case _ =>
        ()
    }
  }

  /** Enumerate the reflected data for properties of an instance. */
  private def reflectingPicklersForProperties(instance: Any)(
      implicit ctx: Context): List[(InspectedData, Pickler)] = {

    val propsData = instance match
      case instance: Reflectable =>
        instance.reflect().reflectProperties(instance)
      case _ =>
        Nil

    for
      data <- propsData
      pickler <- ctx.registry.createPickler(data)
    yield
      (data, pickler)
  }
}
