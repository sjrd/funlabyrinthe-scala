package com.funlabyrinthe.core

abstract class Module(val universe: Universe):
  protected given myUniverse: universe.type = universe

  def dependsOn: Set[ModuleDesc] = Set.empty
end Module
