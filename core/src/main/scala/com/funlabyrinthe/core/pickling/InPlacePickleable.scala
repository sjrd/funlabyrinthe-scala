package com.funlabyrinthe.core.pickling

import com.funlabyrinthe.core.reflect.*

trait InPlacePickleable[-T]:
  def pickle(value: T)(using PicklingContext): Pickle

  def unpickle(value: T, pickle: Pickle)(using PicklingContext): Unit
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
