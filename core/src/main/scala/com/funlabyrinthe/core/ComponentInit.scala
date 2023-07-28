package com.funlabyrinthe.core

final class ComponentInit(val universe: Universe, val id: ComponentID, val owner: ComponentOwner):
  def withID(id: String): ComponentInit =
    ComponentInit(universe, ComponentID(id), owner)
end ComponentInit

object ComponentInit:
  def transient(universe: Universe): ComponentInit =
    ComponentInit(universe, ComponentID.Transient, TransientOwner)
end ComponentInit
