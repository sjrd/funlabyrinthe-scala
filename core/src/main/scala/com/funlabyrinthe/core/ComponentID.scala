package com.funlabyrinthe.core

import language.experimental.macros

class ComponentID(val id: String) extends AnyVal {
  override def toString() = id
}

object ComponentID {
  def apply(id: String): ComponentID = new ComponentID(id)

  implicit def materializeID: ComponentID = macro Macros.materializeID_impl
}
