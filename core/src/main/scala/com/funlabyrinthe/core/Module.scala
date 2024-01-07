package com.funlabyrinthe.core

import scala.quoted.*

import org.portablescala.reflect.annotation.EnableReflectiveInstantiation

import com.funlabyrinthe.core.pickling.Pickleable

@EnableReflectiveInstantiation
abstract class Module(val universe: Universe):
  import Module.*

  protected given myUniverse: universe.type = universe

  def dependsOn: Set[ModuleDesc] = Set.empty

  protected final inline def newAttribute[T](defaultValue: T)(using Pickleable[T]): Attribute[T] =
    universe.newAttribute[T](defaultValue)

  protected final def registerReifiedPlayer[A <: ReifiedPlayer](
    cls: Class[A],
    factory: ReifiedPlayer.Factory[A]
  ): Unit =
    universe.registerReifiedPlayer(cls, factory)
  end registerReifiedPlayer

  inline given materializeComponentInit: ComponentInit =
    ${ materializeComponentInitImpl('{this}) }
end Module

object Module:
  def materializeComponentInitImpl(using Quotes)(module: Expr[Module]): Expr[ComponentInit] =
    import quotes.reflect.*

    val materializedID = ComponentID.materializeIDImpl
    '{ ComponentInit($module.universe, $materializedID, $module) }
  end materializeComponentInitImpl
end Module
