package com.funlabyrinthe.mazes.generic

import com.funlabyrinthe.core.*

final class SimpleSwitchCreator(using ComponentInit) extends ComponentCreator[SimpleSwitch]:
  category = ComponentCategory("customEffects", "Custom Effects")

  icon += "Buttons/SwitchOff"
  icon += "Creators/Creator"
end SimpleSwitchCreator
