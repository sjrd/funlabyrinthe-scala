package com.funlabyrinthe.mazes

import com.funlabyrinthe.core.*
import com.funlabyrinthe.mazes.std.*

final class MapCreator(using ComponentInit) extends ComponentCreator:
  type CreatedComponentType = Map

  category = ComponentCategory("maps", "Maps")

  icon += "Creators/Map"
  icon += "Creators/Creator"

  protected def createComponent()(using init: ComponentInit): CreatedComponentType =
    val result = new Map()
    result.resize(Dimensions(result.zoneWidth, result.zoneHeight, 1), grass(using init.universe))
    result
  end createComponent
end MapCreator
