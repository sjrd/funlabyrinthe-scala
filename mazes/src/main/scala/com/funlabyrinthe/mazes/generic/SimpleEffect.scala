package com.funlabyrinthe.mazes.generic

import com.funlabyrinthe.core.*
import com.funlabyrinthe.mazes.*
import com.funlabyrinthe.mazes.std.PushButton

class SimpleEffect(using ComponentInit) extends Effect derives Reflector:
  var executeInstructions: List[Int] = List(5, 7, 2, 9)
  //var executeInstructions: List[PushButton] = Nil

  override def reflect() = autoReflect[SimpleEffect]
end SimpleEffect
