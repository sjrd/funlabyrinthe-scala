package com.funlabyrinthe.mazes.std

import com.funlabyrinthe.core.graphics.Color
import com.funlabyrinthe.core.inspecting.Inspectable
import com.funlabyrinthe.core.pickling.Pickleable

final case class Lock(color: Color) derives Pickleable, Inspectable
