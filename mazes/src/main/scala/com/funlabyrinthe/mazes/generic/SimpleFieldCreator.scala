package com.funlabyrinthe.mazes.generic

import com.funlabyrinthe.core.*

final class SimpleFieldCreator(using ComponentInit) extends ComponentCreator[SimpleField]:
  category = ComponentCategory("customFields", "Custom Fields")

  icon += "Fields/BrickBuiltWall"
  icon += "Creators/Creator"
end SimpleFieldCreator
