package com.funlabyrinthe.core

import scala.quoted.*

import org.portablescala.reflect.annotation.EnableReflectiveInstantiation

import com.funlabyrinthe.core.pickling.Pickleable
import com.funlabyrinthe.core.inspecting.Inspectable

@EnableReflectiveInstantiation
abstract class Module:
  import Module.*

  private[core] val moduleID: String = getClass().getName().stripSuffix("$")

  override def toString(): String = moduleID

  /** A set of other modules that this module depends on.
   *
   *  For each of the initialization methods (`createComponents`,
   *  `initialize` and `startGame`), the engine will make sure to call the
   *  method on dependees before doing so on the dependants.
   */
  protected def dependsOn: Set[Module] = Set.empty

  /** Performs additional initialization that must be done before creating
   *  components.
   *
   *  This is typically used for:
   *
   *  - creating [[Attribute]]s, and
   *  - registering [[ReifiedPlayer]] classes.
   */
  protected def preInitialize()(using Universe): Unit = ()

  /** Creates the components declared in this module. */
  protected def createComponents()(using Universe): Unit = ()

  /** Performs additional initialization on this module, after all the
   *  components of all the modules have been created.
   *
   *  This is called on every load of a universe, including while editing and
   *  when loading a save file.
   */
  protected def initialize()(using Universe): Unit = ()

  /** Performs additional initialization that should only be done when starting
   *  a new game in playing mode.
   */
  protected def startGame()(using Universe): Unit = ()

  protected final inline def newAttribute[T](
    defaultValue: T,
  )(using universe: Universe, pickleable: Pickleable[T], inspectable: Inspectable[T]): Attribute[T] =
    Attribute.create(universe, this, ComponentInit.materializeID("an attribute ID"), defaultValue, pickleable, inspectable)

  protected final def myAttributeByID[T](id: String)(using universe: Universe): Attribute[T] =
    universe.attributeByID(this, id).asInstanceOf[Attribute[T]]

  protected final def registerReifiedPlayer[A <: ReifiedPlayer](
    cls: Class[A],
    factory: ReifiedPlayer.Factory[A]
  )(using universe: Universe): Unit =
    universe.registerReifiedPlayer(cls, factory)
  end registerReifiedPlayer

  protected inline given materializeComponentInit(using universe: Universe): ComponentInit =
    ComponentInit(universe, ComponentInit.materializeID("a component ID"), ComponentOwner.Module(this))

  protected final def myComponentByID[T <: Component](id: String)(using universe: Universe): T =
    universe.findTopComponentByID[Component](this, id).asInstanceOf[T]
end Module

object Module:
  private[core] def preInitialize(module: Module)(using Universe): Unit =
    module.preInitialize()

  private[core] def createComponents(module: Module)(using Universe): Unit =
    module.createComponents()

  private[core] def initialize(module: Module)(using Universe): Unit =
    module.initialize()

  private[core] def startGame(module: Module)(using Universe): Unit =
    module.startGame()
end Module
