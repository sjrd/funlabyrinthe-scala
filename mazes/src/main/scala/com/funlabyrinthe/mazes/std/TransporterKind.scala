package com.funlabyrinthe.mazes.std

import com.funlabyrinthe.core.inspecting.Inspectable
import com.funlabyrinthe.core.pickling.Pickleable

enum TransporterKind derives Pickleable, Inspectable.StringChoices:
  case Inactive, Next, Previous, Random
