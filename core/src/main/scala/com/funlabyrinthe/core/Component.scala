package com.funlabyrinthe.core

import scala.collection.mutable
import scala.reflect.Typeable

import com.funlabyrinthe.core.pickling.*
import com.funlabyrinthe.core.reflect.*

import graphics._

abstract class Component()(using init: ComponentInit)
    extends Reflectable derives Reflector {

  val universe: Universe = init.universe
  import universe._

  protected given Universe = universe

  private var _id: String = init.id.id
  private[core] var owner: ComponentOwner = init.owner
  private var _category: ComponentCategory = universe.DefaultCategory

  var icon: Painter = EmptyPainter

  universe.componentAdded(this)

  override def reflect() = autoReflect[Component]

  final def id: String = _id

  final def setID(value: String): Unit = {
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

  given ComponentIsPickleable[T <: Component](using Typeable[T]): Pickleable[T] with
    def pickle(value: T)(using Context): Pickle =
      StringPickle(value.id)

    def unpickle(pickle: Pickle)(using Context): Option[T] = pickle match
      case StringPickle(id) =>
        summon[Context].universe.getComponentByIDOption(id) match
          case Some(component: T) => Some(component)
          case _                  => None
      case _ =>
        None
    end unpickle
  end ComponentIsPickleable
}
