package com.funlabyrinthe.mazes.generic

import com.funlabyrinthe.core.*

final class SimplePushButtonCreator(using ComponentInit) extends ComponentCreator[SimplePushButton]:
  category = ComponentCategory("customEffects", "Custom Effects")

  icon += "Buttons/Button"
  icon += "Creators/Creator"
end SimplePushButtonCreator
