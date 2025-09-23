package com.funlabyrinthe.mazes.std

import com.funlabyrinthe.core.graphics.Color
import com.funlabyrinthe.core.inspecting.Inspectable
import com.funlabyrinthe.core.pickling.Pickleable

enum Lock derives Pickleable, Inspectable:
  case NoLock
  case Colored(color: Color)
