package com.funlabyrinthe.mazes

import com.funlabyrinthe.core.*

final class MapCreator()(using Universe, ComponentID) extends ComponentCreator:
  type CreatedComponentType = Map

  def baseID: String = "map"

  def createComponent(id: ComponentID): CreatedComponentType =
    new Map(id)
end MapCreator
