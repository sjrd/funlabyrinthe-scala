package com.funlabyrinthe.core

trait NamedComponent extends Component {
  var name: String = id

  override def toString() = name
}
