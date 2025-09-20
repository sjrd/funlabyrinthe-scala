package com.funlabyrinthe.mazes.generic

import com.funlabyrinthe.core.*

final class SimpleObstacleCreator(using ComponentInit) extends ComponentCreator[SimpleObstacle]:
  category = ComponentCategory("customObstacles", "Custom Obstacles")

  icon += "Blocks/SilverBlock"
  icon += "Creators/Creator"
end SimpleObstacleCreator
