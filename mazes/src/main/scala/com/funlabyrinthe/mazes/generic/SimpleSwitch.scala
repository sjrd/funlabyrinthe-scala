package com.funlabyrinthe.mazes.generic

import com.funlabyrinthe.core.*
import com.funlabyrinthe.mazes.*
import com.funlabyrinthe.mazes.std.Switch

class SimpleSwitch(using ComponentInit) extends Switch derives Reflector:
  category = ComponentCategory("customEffects", "Custom Effects")

  var switchOnInstructions: List[Instruction] = Nil
  var switchOffInstructions: List[Instruction] = Nil

  @noinspect
  var switchOnDoneWithPlayers: Set[Player] = Set.empty
  @noinspect
  var switchOffDoneWithPlayers: Set[Player] = Set.empty

  override def reflect() = autoReflect[SimpleSwitch]

  override def switchOn(context: MoveContext): Unit =
    val firstTime = !switchOnDoneWithPlayers.contains(context.player)
    if firstTime then
      switchOnDoneWithPlayers += context.player
    Instruction.execute(switchOnInstructions, context, firstTime)

  override def switchOff(context: MoveContext): Unit =
    val firstTime = !switchOffDoneWithPlayers.contains(context.player)
    if firstTime then
      switchOffDoneWithPlayers += context.player
    Instruction.execute(switchOffInstructions, context, firstTime)
end SimpleSwitch
