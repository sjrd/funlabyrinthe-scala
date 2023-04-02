package com.funlabyrinthe.core

trait NamedComponent extends Component derives Reflector {
  var name: String = id

  override def reflect() = autoReflect[NamedComponent]

  override def toString() = name
}
