package com.funlabyrinthe.mazes.generic

import com.funlabyrinthe.core.*
import com.funlabyrinthe.mazes.*
import com.funlabyrinthe.mazes.std.PushButton

class SimplePushButton(using ComponentInit) extends PushButton:
  category = ComponentCategory("customEffects", "Custom Effects")

  var buttonDownInstructions: List[Instruction] = Nil
  var buttonUpInstructions: List[Instruction] = Nil

  @noinspect
  var buttonDownDoneWithPlayers: Set[Player] = Set.empty
  @noinspect
  var buttonUpDoneWithPlayers: Set[Player] = Set.empty

  override def buttonDown(context: MoveContext): Unit =
    val firstTime = !buttonDownDoneWithPlayers.contains(context.player)
    if firstTime then
      buttonDownDoneWithPlayers += context.player
    Instruction.execute(buttonDownInstructions, context, firstTime)

  override def buttonUp(context: MoveContext): Unit =
    val firstTime = !buttonUpDoneWithPlayers.contains(context.player)
    if firstTime then
      buttonUpDoneWithPlayers += context.player
    Instruction.execute(buttonUpInstructions, context, firstTime)
end SimplePushButton
