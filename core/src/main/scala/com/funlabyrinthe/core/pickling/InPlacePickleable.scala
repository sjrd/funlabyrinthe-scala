package com.funlabyrinthe.core.pickling

import com.funlabyrinthe.core.reflect.*

trait InPlacePickleable[-T]:
  def pickle(value: T)(using PicklingContext): Pickle

  def unpickle(value: T, pickle: Pickle)(using PicklingContext): Unit

  private object pickler extends Pickler:
    def pickle(data: InspectedData)(implicit ctx: PicklingContext): Pickle =
      InPlacePickleable.this.pickle(data.value.asInstanceOf[T])

    def unpickle(data: InspectedData, pickle: Pickle)(implicit ctx: PicklingContext): Unit =
      InPlacePickleable.this.unpickle(data.value.asInstanceOf[T], pickle)
  end pickler

  final def toPickler: Pickler = pickler
end InPlacePickleable

object InPlacePickleable:
  def pickle[T](value: T)(using PicklingContext, InPlacePickleable[T]): Pickle =
    summon[InPlacePickleable[T]].pickle(value)

  def unpickle[T](value: T, pickle: Pickle)(using PicklingContext, InPlacePickleable[T]): Unit =
    summon[InPlacePickleable[T]].unpickle(value, pickle)

  given ForReflectable: InPlacePickleable[Reflectable] with
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
  end ForReflectable
end InPlacePickleable
