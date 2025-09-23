package com.funlabyrinthe.mazes

import com.funlabyrinthe.core.*

final class ItemToolCreator(using ComponentInit) extends ComponentCreator[ItemTool]:
  category = ComponentCategory("customTools", "Custom Tools")

  icon += "Objects/SilverKey"
  icon += "Creators/Creator"
end ItemToolCreator
