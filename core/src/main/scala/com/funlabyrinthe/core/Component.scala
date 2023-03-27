package com.funlabyrinthe.core

import scala.collection.mutable

import graphics._

abstract class Component()(implicit val universe: Universe,
    originalID: ComponentID) {

  def this(id: ComponentID)(implicit universe: Universe) =
    this()(universe, id)

  import universe._

  private var _id: String = originalID.id
  private var _category: ComponentCategory = universe.DefaultCategory

  var icon: Painter = EmptyPainter

  universe.componentAdded(this)

  final def id: String = _id
  final def id_=(value: String): Unit = {
    if (value != _id) {
      require(Component.isValidIDOpt(value),
          s"'${value}' is not a valid component identifier")

      require(!universe.componentIDExists(value),
          s"Duplicate component identifier '${value}'")

      val old = _id
      _id = value

      universe.componentIDChanged(this, old, value)

      onIDChanged(old, _id)
    }
  }

  final def category: ComponentCategory = _category
  final protected def category_=(value: ComponentCategory): Unit = {
    _category = value
  }

  def onIDChanged(oldID: String, newID: String): Unit = ()

  override def toString() = id

  def drawIcon(context: DrawContext): Unit = {
    if (icon != EmptyPainter)
      icon.drawTo(context)
    else
      DefaultIconPainter.drawTo(context)
  }
}

object Component {
  val IconWidth = 48
  val IconHeight = 48

  def isValidID(id: String): Boolean = {
    !id.isEmpty
    //(!id.isEmpty() && isIDStart(id.charAt(0)) && id.forall(isIDPart))
  }

  def isValidIDOpt(id: String): Boolean =
    true
    //id.isEmpty() || isValidID(id)

  //def isIDStart(c: Char) = c.isUnicodeIdentifierStart
  //def isIDPart(c: Char) = c.isUnicodeIdentifierPart || c == '#'
}
