package com.funlabyrinthe.core

import scala.collection.mutable
import scala.reflect.Typeable

import com.funlabyrinthe.core.pickling.*
import com.funlabyrinthe.core.reflect.*

import graphics._

abstract class Component()(using init: ComponentInit)
    extends Reflectable derives Reflector {
  import Component.*

  val universe: Universe = init.universe
  import universe._

  protected given Universe = universe

  private var _id: String = init.id
  private[core] var owner: ComponentOwner = init.owner
  private var _category: ComponentCategory = universe.DefaultCategory

  @transient
  protected var icon: Painter = EmptyPainter

  /** Visual text tag only visible during editing. */
  var editVisualTag: String = ""

  universe.componentAdded(this)

  override def reflect() = autoReflect[Component]

  @transient @noinspect
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

  @transient @noinspect
  final def category: ComponentCategory = _category
  final protected def category_=(value: ComponentCategory): Unit = {
    _category = value
  }

  def onIDChanged(oldID: String, newID: String): Unit = ()

  override final def toString(): String = id

  def drawIcon(context: DrawContext): Unit = {
    val effectivePainter =
      if icon != EmptyPainter then icon
      else DefaultIconPainter

    effectivePainter.drawTo(context)
    drawEditVisualTag(context)
  }

  protected final def drawEditVisualTag(context: DrawContext): Unit =
    if universe.isEditing && editVisualTag.nonEmpty then
      val gc = context.gc

      val (w, h) = universe.graphicsSystem.measureText(editVisualTag, editVisualTagFont)

      val textX = (context.minX + context.maxX - w) / 2
      val textY = (context.minY + context.maxY - h) / 2

      gc.fill = Color.White
      gc.fillRect(textX - 1, textY - 1, w + 2, h + 2)

      gc.fill = Color.Black
      gc.font = editVisualTagFont
      gc.fillText(editVisualTag, textX, textY)
  end drawEditVisualTag
}

object Component {
  val IconWidth = 48
  val IconHeight = 48

  private val editVisualTagFont =
    Font(List("Arial"), 11)

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
    def pickle(value: T)(using PicklingContext): Pickle =
      StringPickle(value.id)

    def unpickle(pickle: Pickle)(using PicklingContext): Option[T] = pickle match
      case StringPickle(id) =>
        summon[PicklingContext].universe.getComponentByIDOption(id) match
          case Some(component: T) => Some(component)
          case _                  => None
      case _ =>
        None
    end unpickle
  end ComponentIsPickleable
}
