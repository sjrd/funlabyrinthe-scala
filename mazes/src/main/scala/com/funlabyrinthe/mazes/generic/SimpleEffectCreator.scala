package com.funlabyrinthe.mazes.generic

import com.funlabyrinthe.core.*
import com.funlabyrinthe.mazes.*

final class SimpleEffectCreator(using ComponentInit) extends ComponentCreator[SimpleEffect]:
  category = ComponentCategory("effects", "Effects")

  icon += "Arrows/EastArrow"
  icon += "Creators/Creator"
end SimpleEffectCreator
