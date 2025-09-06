package com.funlabyrinthe.mazes.std

import com.funlabyrinthe.core.*
import com.funlabyrinthe.mazes.*

final class TransporterCreator(using ComponentInit) extends ComponentCreator[Transporter]:
  category = ComponentCategory("transporters", "Transporters")

  icon += "Transporters/Transporter"
  icon += "Creators/Creator"
end TransporterCreator
