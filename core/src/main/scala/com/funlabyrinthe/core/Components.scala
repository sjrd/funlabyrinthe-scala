package com.funlabyrinthe.core

import scala.collection.mutable

trait Components { universe: Universe =>
  private val _components = new mutable.ArrayBuffer[Component]
  private val _componentsByID = new mutable.HashMap[String, Component]

  def components = _components.toIndexedSeq

  private def componentAdded(component: Component) {
    _components += component
    if (!component.id.isEmpty())
      _componentsByID += component.id -> component
  }

  private def componentIDExists(id: String) = _componentsByID contains id

  def getComponentByID(id: String): Component = {
    _componentsByID(id)
  }

  abstract class Component {
    private var _id: String = computeDefaultID()

    Components.this.componentAdded(this)

    final def id: String = _id
    final def id_=(value: String) {
      if (value != _id) {
        require(Components.isValidIDOpt(value),
            s"'${value}' is not a valid component identifier")

        require(!componentIDExists(value),
            s"Duplicate component identifier '${value}'")

        val old = _id
        _id = value

        _componentsByID -= old
        if (!value.isEmpty())
          _componentsByID += value -> this

        onIDChanged(old, _id)
      }
    }

    def onIDChanged(oldID: String, newID: String): Unit = ()

    override def toString() = id

    protected[this] def computeDefaultID(): String = {
      val (base, tryWithoutSuffix) = computeDefaultIDBase()

      if (base.isEmpty()) base
      else if (tryWithoutSuffix && !componentIDExists(base)) base
      else {
        var suffix = 1
        while (componentIDExists(base+suffix))
          suffix += 1
        base+suffix
      }
    }

    protected[this] def computeDefaultIDBase(): (String, Boolean) = {
      val simpleName = scalaFriendlyClassSimpleName(getClass)

      if (simpleName.isEmpty()) ("", false)
      else if (simpleName.last == '$') (simpleName.init, true)
      else (simpleName, false)
    }

    private def scalaFriendlyClassSimpleName(cls: Class[_]): String = {
      def isAsciiDigit(c: Char) = '0' <= c && c <= '9'

      val enclosingCls = cls.getEnclosingClass
      val clsName = cls.getName

      if (enclosingCls eq null) {
        // Strip the package name
        clsName.substring(clsName.lastIndexOf('.')+1)
      } else {
        // Strip the enclosing class name and any leading "\$?[0-9]*"
        val length = clsName.length
        var start = enclosingCls.getName.length
        if (start < length && clsName.charAt(start) == '$')
          start += 1
        while (start < length && isAsciiDigit(clsName.charAt(start)))
          start += 1
        clsName.substring(start) // will be "" for an anonymous class
      }
    }
  }

  trait NamedComponent extends Component {
    var name: String = id

    override def toString() = name
  }

  trait VisualComponent extends NamedComponent {
    var painter: Painter = EmptyPainter

    def drawTo(context: DrawContext) {
      painter.drawTo(context)
    }
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
