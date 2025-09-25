package com.funlabyrinthe.core

import scala.collection.mutable
import scala.reflect.{ClassTag, Typeable}

import scala.scalajs.reflect.annotation.EnableReflectiveInstantiation

import com.funlabyrinthe.core.pickling.*
import com.funlabyrinthe.core.reflect.*

import graphics._

@EnableReflectiveInstantiation
abstract class Component()(using init: ComponentInit) extends Reflectable {
  import Component.*

  val universe: Universe = init.universe
  import universe._

  protected given Universe = universe

  private var _id: String = init.id
  private[core] val owner: ComponentOwner = init.owner
  private var _category: ComponentCategory = ComponentCategory("default", "Default")

  private var _icon: Painter = EmptyPainter

  @transient @noinspect
  protected def icon: Painter = _icon
  protected def icon_=(value: Painter): Unit = _icon = value

  /** Visual text tag only visible during editing. */
  var editVisualTag: String = ""

  @transient @noinspect
  def isTransient: Boolean = _id.isEmpty()

  @transient @noinspect
  def isAdditional: Boolean = owner match
    case ComponentOwner.Module(AdditionalComponents) => true
    case _                                           => false

  @transient @noinspect
  def fullID: String =
    val ownerFullID = owner match
      case ComponentOwner.Module(module)   => module.moduleID
      case ComponentOwner.Component(owner) => owner.fullID

    if id.isEmpty() then
      throw IllegalArgumentException(
        "Cannot save because there is a reference to a transient subcomponent "
          + s"of $ownerFullID of class ${getClass().getName()}"
      )

    ownerFullID + ":" + id
  end fullID

  private val _subComponents: mutable.ListBuffer[Component] = mutable.ListBuffer.empty
  private val _subComponentsByID: mutable.HashMap[String, Component] = mutable.HashMap.empty

  private[Component] def subComponentAdded(subComponent: Component): Unit =
    universe.subComponentAdded(subComponent)
    _subComponents += subComponent
    if !subComponent.id.isEmpty() then
      _subComponentsByID += subComponent.id -> subComponent
  end subComponentAdded

  private[Component] def subComponentIDChanging(subComponent: Component, newID: String): Unit =
    require(!_subComponentsByID.contains(newID), s"Duplicate subcomponent identifier '$newID")
    _subComponentsByID -= subComponent.id
    _subComponentsByID += newID -> subComponent

  def lookupSubComponentByID(id: String): Option[Component] =
    _subComponentsByID.get(id)

  owner match
    case ComponentOwner.Module(module)   => universe.topComponentAdded(module, this)
    case ComponentOwner.Component(owner) => owner.subComponentAdded(this)

  private[core] def storeDefaultsAllSubComponents(): Unit =
    InPlacePickleable.storeDefaults(this)
    for subComponent <- _subComponents do
      subComponent.storeDefaultsAllSubComponents()
  end storeDefaultsAllSubComponents

  @transient
  final def id: String = _id

  final def id_=(value: String): Unit = {
    if (value != _id) {
      if _id.isEmpty() then
        throw IllegalArgumentException("Cannot change the ID of a transient component")
      if !isAdditional then
        throw IllegalArgumentException(
          s"The ID of the component $_id is fixed and cannot be changed. "
            + "You can only change the ID of additonal components."
        )

      require(Component.isValidID(value), s"'${value}' is not a valid component ID")

      owner match
        case ComponentOwner.Module(module) =>
          universe.topComponentIDChanging(module, this, value)
        case ComponentOwner.Component(owner) =>
          // This is currently dead code
          owner.subComponentIDChanging(this, value)

      val old = _id
      _id = value
      onIDChanged(old, _id)
    }
  }

  protected final inline def subComponent[A <: Component](inline f: ComponentInit ?=> A): A =
    f(using ComponentInit(universe, ComponentInit.materializeID("a component ID"), ComponentOwner.Component(this)))

  protected final inline def transientComponent[A <: Component](inline f: ComponentInit ?=> A): A =
    f(using ComponentInit(universe, "", ComponentOwner.Component(this)))

  @transient @noinspect
  final def category: ComponentCategory = _category
  final protected def category_=(value: ComponentCategory): Unit = {
    _category = value
  }

  def onIDChanged(oldID: String, newID: String): Unit = ()

  /** Called when this component has been destroyed.
   *
   *  Override this method if you need to explicitly remove this component from
   *  custom caches.
   */
  protected def onDestroyed(): Unit = ()

  override final def toString(): String = id

  def drawIcon(context: DrawContext): Unit = {
    val effectivePainter =
      if icon != EmptyPainter then icon
      else DefaultIconPainter

    effectivePainter.drawStretchedTo(context)
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

  private[core] def onDestroyedInternal(component: Component): Unit =
    component.onDestroyed()

  given ComponentIsPickleable[T <: Component](using Typeable[T], ClassTag[T]): Pickleable[T] with
    def pickle(value: T)(using PicklingContext): Pickle =
      StringPickle(value.fullID)

    def unpickle(pickle: Pickle)(using PicklingContext): Option[T] = pickle match
      case StringPickle(id) =>
        summon[PicklingContext].universe.lookupNestedComponentByFullID(id) match
          case Some(component) =>
            summon[Typeable[T]].unapply(component).orElse {
              PicklingContext.typeError(
                s"component of class ${summon[ClassTag[T]].runtimeClass.getName()}",
                s"component $id of class ${component.getClass().getName()}"
              )
            }
          case _ =>
            PicklingContext.error(s"unknown component ID: $id")
      case _ =>
        PicklingContext.typeError("component ID string", pickle)
    end unpickle

    def removeReferences(value: T, reference: Component)(using PicklingContext): Pickleable.RemoveRefResult[T] =
      if value eq reference then Pickleable.RemoveRefResult.Failure
      else Pickleable.RemoveRefResult.Unchanged
  end ComponentIsPickleable

  given ComponentOrdering[T <: Component]: Ordering[T] with
    def compare(x: T, y: T): Int =
      (x.isTransient, y.isTransient) match
        case (false, false) => x.fullID.compareTo(y.fullID)
        case (false, true)  => 1
        case (true, false)  => 0
        case (true, true)   => java.lang.Integer.compare(x.##, y.##) // if they're transient, they will stay in this process
  end ComponentOrdering
}
