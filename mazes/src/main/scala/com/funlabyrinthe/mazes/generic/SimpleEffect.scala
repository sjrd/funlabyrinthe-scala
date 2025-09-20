package com.funlabyrinthe.mazes.generic

import com.funlabyrinthe.core.*
import com.funlabyrinthe.mazes.*
import com.funlabyrinthe.mazes.std.PushButton

class SimpleEffect(using ComponentInit) extends Effect derives Reflector:
  category = ComponentCategory("customEffects", "Custom Effects")

  var executeInstructions: List[Instruction] = Nil

  @noinspect
  var executeDoneWithPlayers: Set[Player] = Set.empty

  override def reflect() = autoReflect[SimpleEffect]

  override def execute(context: MoveContext): Unit =
    val firstTime = !executeDoneWithPlayers.contains(context.player)
    if firstTime then
      executeDoneWithPlayers += context.player
    Instruction.execute(executeInstructions, context, firstTime)
end SimpleEffect
