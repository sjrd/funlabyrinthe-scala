package com.funlabyrinthe.core

final class ComponentInit(val universe: Universe, val id: ComponentID, val owner: ComponentOwner):
  def withID(id: String): ComponentInit =
    ComponentInit(universe, ComponentID(id), owner)
end ComponentInit
