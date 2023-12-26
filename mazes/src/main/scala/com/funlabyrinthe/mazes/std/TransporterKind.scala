package com.funlabyrinthe.mazes.std

import com.funlabyrinthe.core.pickling.Pickleable

enum TransporterKind derives Pickleable:
  case Inactive, Next, Previous, Random
