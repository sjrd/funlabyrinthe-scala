package com.funlabyrinthe.mazes.generic

import com.funlabyrinthe.core.pickling.Pickleable
import com.funlabyrinthe.core.inspecting.Inspectable

enum ObstacleCondition derives Pickleable, Inspectable:
  case NeverDestroy
  case AlwaysDestroy
  // TODO Destroy if ability
end ObstacleCondition
