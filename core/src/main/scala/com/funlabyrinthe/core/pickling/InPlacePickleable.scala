package com.funlabyrinthe.core.pickling

import com.funlabyrinthe.core.reflect.*

trait InPlacePickleable[-T]:
  def pickle(value: T)(using Context): Pickle

  def unpickle(value: T, pickle: Pickle)(using Context): Unit

  private object pickler extends Pickler:
    def pickle(data: InspectedData)(implicit ctx: Context): Pickle =
      InPlacePickleable.this.pickle(data.value.asInstanceOf[T])

    def unpickle(data: InspectedData, pickle: Pickle)(implicit ctx: Context): Unit =
      InPlacePickleable.this.unpickle(data.value.asInstanceOf[T], pickle)
  end pickler

  final def toPickler: Pickler = pickler
end InPlacePickleable

object InPlacePickleable:
  given ForReflectable: InPlacePickleable[Reflectable] with
    def pickle(value: Reflectable)(using Context): Pickle =
      ObjectPickle(value.save().toList)
    end pickle

    def unpickle(value: Reflectable, pickle: Pickle)(using Context): Unit =
      pickle match {
        case ObjectPickle(fields) =>
          value.load(fields.toMap)
        case _ =>
          ()
      }
    end unpickle
  end ForReflectable
end InPlacePickleable
