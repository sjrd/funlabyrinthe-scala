package com.funlabyrinthe.mazes

import com.funlabyrinthe.core.*
import com.funlabyrinthe.mazes.std.*

final class MapCreator(using ComponentInit) extends ComponentCreator[Map]:
  category = ComponentCategory("maps", "Maps")

  icon += "Maps/MazeMap"
  icon += "Creators/Creator"

  override protected def initializeNewComponent(component: Map): Unit =
    super.initializeNewComponent(component)
    component.resize(Dimensions(component.zoneWidth, component.zoneHeight, 1), grass)
end MapCreator
