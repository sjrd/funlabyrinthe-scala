package com.funlabyrinthe.core

import scala.quoted.*

import com.funlabyrinthe.core.inspecting.Inspectable
import com.funlabyrinthe.core.pickling.Pickleable

final class Attribute[T] private (
  val owner: Module,
  val id: String,
  val defaultValue: T,
  val pickleable: Pickleable[T],
  val inspectable: Inspectable[T],
):
  override def toString(): String = id

  def fullID: String = owner.moduleID + ":" + id
end Attribute

object Attribute:
  inline def create[T](defaultValue: T)(
    using universe: Universe, pickleable: Pickleable[T], inspectable: Inspectable[T]
  ): Attribute[T] =
    createInternal(universe, ComponentInit.autoModuleOwner,
        ComponentInit.materializeID("an attribute ID"),
        defaultValue, pickleable, inspectable)
  end create

  private[core] def createInternal[T](
    universe: Universe,
    owner: Module,
    id: String,
    defaultValue: T,
    pickleable: Pickleable[T],
    inspectable: Inspectable[T],
  ): Attribute[T] =
    universe.registerAttribute(owner, new Attribute(owner, id, defaultValue, pickleable, inspectable))
  end createInternal
end Attribute
