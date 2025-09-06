package com.funlabyrinthe.core.pickling

import scala.collection.mutable.Builder

import com.funlabyrinthe.core.Component

trait InPlacePickleable[-T]:
  def storeDefaults(value: T): Unit

  def pickle(value: T)(using PicklingContext): Option[Pickle]

  def unpickle(value: T, pickle: Pickle)(using PicklingContext): Unit

  def prepareRemoveReferences(value: T, reference: Component, actions: InPlacePickleable.PreparedActions)(
      using PicklingContext): Unit
end InPlacePickleable

object InPlacePickleable:
  trait PreparedActions:
    def prepare(action: => Unit): Unit

  def storeDefaults[T](value: T)(using InPlacePickleable[T]): Unit =
    summon[InPlacePickleable[T]].storeDefaults(value)

  def pickle[T](value: T)(using PicklingContext, InPlacePickleable[T]): Option[Pickle] =
    summon[InPlacePickleable[T]].pickle(value)

  def unpickle[T](value: T, pickle: Pickle)(using PicklingContext, InPlacePickleable[T]): Unit =
    summon[InPlacePickleable[T]].unpickle(value, pickle)

  def prepareRemoveReferences[T](value: T, reference: Component, actions: PreparedActions)(
      using PicklingContext, InPlacePickleable[T]): Unit =
    summon[InPlacePickleable[T]].prepareRemoveReferences(value, reference, actions)
end InPlacePickleable
