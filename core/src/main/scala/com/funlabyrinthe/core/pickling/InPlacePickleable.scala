package com.funlabyrinthe.core.pickling

trait InPlacePickleable[-T]:
  def storeDefaults(value: T): Unit

  def pickle(value: T)(using PicklingContext): Option[Pickle]

  def unpickle(value: T, pickle: Pickle)(using PicklingContext): Unit
end InPlacePickleable

object InPlacePickleable:
  def storeDefaults[T](value: T)(using InPlacePickleable[T]): Unit =
    summon[InPlacePickleable[T]].storeDefaults(value)

  def pickle[T](value: T)(using PicklingContext, InPlacePickleable[T]): Option[Pickle] =
    summon[InPlacePickleable[T]].pickle(value)

  def unpickle[T](value: T, pickle: Pickle)(using PicklingContext, InPlacePickleable[T]): Unit =
    summon[InPlacePickleable[T]].unpickle(value, pickle)
end InPlacePickleable
