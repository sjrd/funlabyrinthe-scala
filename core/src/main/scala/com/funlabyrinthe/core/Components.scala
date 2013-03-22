package com.funlabyrinthe.core

import scala.collection.mutable

trait Components { universe: Universe =>
  private val _components = new mutable.ArrayBuffer[Component]

  def components = _components.toIndexedSeq

  private def componentAdded(component: Component) {
    _components += component
  }

  abstract class Component {
    private var _id: String = ""

    final def id: String = _id
    final def id_=(value: String) {
      require(Components.isValidIDOpt(value),
          s"'${value}' is not a valid component identifier")
      val old = _id
      _id = old
      onIDChanged(old, _id)
    }

    def onIDChanged(oldID: String, newID: String): Unit = ()

    Components.this.componentAdded(this)
  }

  trait NamedComponent extends Component {
    var name: String
  }

  trait VisualComponent {
    val painter = new graphics.Painter
  }
}

object Components {
  def isValidID(id: String): Boolean = {
    (!id.isEmpty() && id.charAt(0).isUnicodeIdentifierStart &&
        id.forall(_.isUnicodeIdentifierPart))
  }

  def isValidIDOpt(id: String): Boolean =
    id.isEmpty() || isValidID(id)
}
