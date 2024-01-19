package com.funlabyrinthe.core

import scala.quoted.*

import org.portablescala.reflect.annotation.EnableReflectiveInstantiation

import com.funlabyrinthe.core.pickling.Pickleable
import com.funlabyrinthe.core.inspecting.Inspectable

@EnableReflectiveInstantiation
abstract class Module:
  import Module.*

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
  )(using Universe, Pickleable[T], Inspectable[T]): Attribute[T] =
    summon[Universe].newAttribute[T](defaultValue)

  protected final def registerReifiedPlayer[A <: ReifiedPlayer](
    cls: Class[A],
    factory: ReifiedPlayer.Factory[A]
  )(using universe: Universe): Unit =
    universe.registerReifiedPlayer(cls, factory)
  end registerReifiedPlayer

  inline given materializeComponentInit(using universe: Universe): ComponentInit =
    ${ materializeComponentInitImpl('universe, '{this}) }
end Module

object Module:
  def materializeComponentInitImpl(using Quotes)(universe: Expr[Universe], module: Expr[Module]): Expr[ComponentInit] =
    import quotes.reflect.*

    val materializedID = ComponentInit.materializeIDImpl("a component ID")
    '{ ComponentInit($universe, $materializedID, $module) }
  end materializeComponentInitImpl

  private[core] def preInitialize(module: Module)(using Universe): Unit =
    module.preInitialize()

  private[core] def createComponents(module: Module)(using Universe): Unit =
    module.createComponents()

  private[core] def initialize(module: Module)(using Universe): Unit =
    module.initialize()

  private[core] def startGame(module: Module)(using Universe): Unit =
    module.startGame()
end Module
