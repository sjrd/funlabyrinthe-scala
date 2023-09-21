package com.funlabyrinthe.core.pickling

import com.funlabyrinthe.core.Universe

final class PicklingRegistry(val universe: Universe):
  def pickle[T](value: T)(using InPlacePickleable[T]): Pickle = {
    implicit val context = createContext()
    summon[InPlacePickleable[T]].pickle(value)
  }

  def unpickle[T](value: T, pickle: Pickle)(using InPlacePickleable[T]): Unit = {
    implicit val context = createContext()
    summon[InPlacePickleable[T]].unpickle(value, pickle)
  }

  private def createContext() = {
    new Context {
      val registry = PicklingRegistry.this
    }
  }
end PicklingRegistry
