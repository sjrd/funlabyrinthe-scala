package com.funlabyrinthe.core

import scala.quoted.*

abstract class Module(val universe: Universe):
  import Module.*

  protected given myUniverse: universe.type = universe

  def dependsOn: Set[ModuleDesc] = Set.empty

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
