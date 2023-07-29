package com.funlabyrinthe.mazes.std

import com.funlabyrinthe.core.*
import com.funlabyrinthe.mazes.*

final class BoatCreator(using ComponentInit) extends ComponentCreator:
  type CreatedComponentType = Boat

  icon += "Vehicles/Boat"

  def baseID: String = "Boat"

  protected def createComponent()(using init: ComponentInit): CreatedComponentType =
    new Boat()
end BoatCreator
