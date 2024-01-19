package com.funlabyrinthe.core

import scala.quoted.*

import com.funlabyrinthe.core.inspecting.Inspectable
import com.funlabyrinthe.core.pickling.Pickleable

final class Attribute[T] private (
  val name: String,
  val defaultValue: T,
  val pickleable: Pickleable[T],
  val inspectable: Inspectable[T],
):
  override def toString(): String = name
end Attribute

object Attribute:
  private[core] def create[T](
    universe: Universe,
    name: String,
    defaultValue: T,
    pickleable: Pickleable[T],
    inspectable: Inspectable[T],
  ): Attribute[T] =
    universe.registerAttribute(new Attribute(name, defaultValue, pickleable, inspectable))
  end create
end Attribute
