package com.funlabyrinthe.mazes.std

import com.funlabyrinthe.core.*
import com.funlabyrinthe.mazes.*

final class TransporterCreator(using ComponentInit) extends ComponentCreator:
  type CreatedComponentType = Transporter

  category = ComponentCategory("transporters", "Transporters")

  icon += "Transporters/Transporter"
  icon += "Creators/Creator"

  def baseID: String = "Transporter"

  protected def createComponent()(using init: ComponentInit): CreatedComponentType =
    new Transporter()
end TransporterCreator
