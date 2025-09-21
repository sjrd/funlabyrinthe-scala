package com.funlabyrinthe.core

abstract class NamedComponent(using ComponentInit) extends Component {
  var name: String = id
}
