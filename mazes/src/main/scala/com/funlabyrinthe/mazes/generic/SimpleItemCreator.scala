package com.funlabyrinthe.mazes.generic

import com.funlabyrinthe.core.*

final class SimpleItemCreator(using ComponentInit) extends ComponentCreator[SimpleItem]:
  category = ComponentCategory("customItems", "Custom Items")

  icon += "Objects/SilverKey"
  icon += "Creators/Creator"
end SimpleItemCreator
