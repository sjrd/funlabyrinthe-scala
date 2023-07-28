package com.funlabyrinthe.core

abstract class NamedComponent(using ComponentInit) extends Component derives Reflector {
  var name: String = id

  override def reflect() = autoReflect[NamedComponent]

  override def toString() = name
}
