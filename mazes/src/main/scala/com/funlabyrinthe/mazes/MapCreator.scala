package com.funlabyrinthe.mazes

import com.funlabyrinthe.core.*

final class MapCreator(using ComponentInit) extends ComponentCreator:
  type CreatedComponentType = Map

  def baseID: String = "map"

  protected def createComponent()(using init: ComponentInit): CreatedComponentType =
    new Map()
end MapCreator
