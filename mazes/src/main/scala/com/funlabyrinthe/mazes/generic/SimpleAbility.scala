package com.funlabyrinthe.mazes.generic

import com.funlabyrinthe.core.Ability
import com.funlabyrinthe.core.inspecting.Inspectable
import com.funlabyrinthe.core.pickling.Pickleable

final case class SimpleAbility(id: String) extends Ability derives Pickleable, Inspectable
