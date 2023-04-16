package com.funlabyrinthe.editor.pickling

import com.funlabyrinthe.core.reflect.*

trait InPlacePickleable[-T]:
  def pickle(value: T)(using Context): Pickle

  def unpickle(value: T, pickle: Pickle)(using Context): Unit
end InPlacePickleable

object InPlacePickleable:
  given ForReflectable: InPlacePickleable[Reflectable] with
    def pickle(value: Reflectable)(using Context): Pickle =
      val pickledFields = for {
        (propData, propPickler) <- reflectingPicklersForProperties(value)
      } yield {
        (propData.name, propPickler.pickle(propData))
      }

      ObjectPickle(pickledFields)
    end pickle

    def unpickle(value: Reflectable, pickle: Pickle)(using Context): Unit =
      pickle match {
        case ObjectPickle(pickleFields) =>
          val pickleMap = Map(pickleFields:_*)

          for {
            (propData, propPickler) <- reflectingPicklersForProperties(value)
          } {
            pickleMap.get(propData.name) foreach { propPickle =>
              propPickler.unpickle(propData, propPickle)
            }
          }

        case _ =>
          ()
      }
    end unpickle

    /** Enumerate the reflected data for properties of an instance. */
    private def reflectingPicklersForProperties(value: Reflectable)(
        using Context): List[(InspectedData, Pickler)] =

      for
        data <- value.reflect().reflectProperties(value)
        pickler <- summon[Context].registry.createPickler(data)
      yield
        (data, pickler)
    end reflectingPicklersForProperties
  end ForReflectable
end InPlacePickleable
