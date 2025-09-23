package com.funlabyrinthe.mazes.generic

import com.funlabyrinthe.core.Ability
import com.funlabyrinthe.core.pickling.Pickleable
import com.funlabyrinthe.core.inspecting.Inspectable

enum FieldCondition derives Pickleable, Inspectable:
  case AlwaysAllow
  case NeverAllow
  case AllowIfAbility(ability: Ability)
end FieldCondition
