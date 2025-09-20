package com.funlabyrinthe.mazes.generic

import com.funlabyrinthe.core.*

final class SimpleEffectCreator(using ComponentInit) extends ComponentCreator[SimpleEffect]:
  category = ComponentCategory("customEffects", "Custom Effects")

  icon += "Arrows/EastArrow"
  icon += "Creators/Creator"
end SimpleEffectCreator
