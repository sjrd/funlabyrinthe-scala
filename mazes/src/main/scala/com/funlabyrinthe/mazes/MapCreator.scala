package com.funlabyrinthe.mazes

import com.funlabyrinthe.core.*

final class MapCreator(using ComponentInit) extends ComponentCreator:
  type CreatedComponentType = Map

  category = ComponentCategory("maps", "Maps")

  icon += "Creators/Map"
  icon += "Creators/Creator"

  protected def createComponent()(using init: ComponentInit): CreatedComponentType =
    val result = new Map()
    result.resize(Dimensions(result.zoneWidth, result.zoneHeight, 1), Mazes.mazes.grass)
    result
  end createComponent
end MapCreator
