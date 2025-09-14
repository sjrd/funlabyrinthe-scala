package com.funlabyrinthe.mazes.generic

import com.funlabyrinthe.core.*
import com.funlabyrinthe.core.pickling.Pickleable

import com.funlabyrinthe.mazes.*
import com.funlabyrinthe.mazes.std.*

enum Instruction derives Pickleable:
  case ChangeSquare(
    map: Map,
    position: Position,
    field: Option[Field],
    effect: Option[Effect],
    tool: Option[Tool],
    obstacle: Option[Obstacle],
  )

  case ChangeButtonEnabled(button: PushButton, enabled: Boolean)
end Instruction

object Instruction:
  def execute(context: MoveContext, instructions: List[Instruction])(using Universe): Unit =
    for instruction <- instructions do
      execute(context, instruction)

  def execute(context: MoveContext, instruction: Instruction)(using Universe): Unit =
    instruction match
      case ChangeSquare(map, position, field, effect, tool, obstacle) =>
        var newSquare = map(position)
        for f <- field do
          newSquare += f
        for e <- effect do
          newSquare += e
        for t <- tool do
          newSquare += t
        for o <- obstacle do
          newSquare += o
        map(position) = newSquare

      case ChangeButtonEnabled(button, enabled) =>
        button.enabled = enabled
  end execute
