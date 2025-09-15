package com.funlabyrinthe.mazes.generic

import com.funlabyrinthe.core.*
import com.funlabyrinthe.mazes.*

class SimpleEffect(using ComponentInit) extends Effect derives Reflector:
  var executeInstructions: List[Int] = List(5, 7, 2, 9)

  override def reflect() = autoReflect[SimpleEffect]
end SimpleEffect
