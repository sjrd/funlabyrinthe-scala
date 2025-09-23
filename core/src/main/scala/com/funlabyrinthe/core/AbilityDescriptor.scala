package com.funlabyrinthe.core

import com.funlabyrinthe.core.inspecting.Inspectable
import com.funlabyrinthe.core.pickling.Pickleable

private[core] final class AbilityDescriptor[T <: Ability](
  val cls: Class[T],
  val pickleable: Pickleable[T],
  val inspectable: Inspectable[T],
):
  val className: String = cls.getName()
end AbilityDescriptor
