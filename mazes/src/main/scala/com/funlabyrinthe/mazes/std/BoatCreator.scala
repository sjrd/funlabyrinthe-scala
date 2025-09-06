package com.funlabyrinthe.mazes.std

import com.funlabyrinthe.core.*
import com.funlabyrinthe.mazes.*

final class BoatCreator(using ComponentInit) extends ComponentCreator[Boat]:
  category = ComponentCategory("boats", "Boats")

  icon += "Vehicles/Boat"
  icon += "Creators/Creator"
end BoatCreator
