package com.funlabyrinthe.core.pickling

trait InPlacePickleable[-T]:
  def pickle(value: T)(using PicklingContext): Option[Pickle]

  def unpickle(value: T, pickle: Pickle)(using PicklingContext): Unit
end InPlacePickleable

object InPlacePickleable:
  def pickle[T](value: T)(using PicklingContext, InPlacePickleable[T]): Option[Pickle] =
    summon[InPlacePickleable[T]].pickle(value)

  def unpickle[T](value: T, pickle: Pickle)(using PicklingContext, InPlacePickleable[T]): Unit =
    summon[InPlacePickleable[T]].unpickle(value, pickle)
end InPlacePickleable
