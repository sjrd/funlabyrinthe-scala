package com.funlabyrinthe.mazes.generic

import com.funlabyrinthe.core.*
import com.funlabyrinthe.core.scene.RGBA
import com.funlabyrinthe.core.sounds.*
import com.funlabyrinthe.core.inspecting.Inspectable
import com.funlabyrinthe.core.pickling.Pickleable

import com.funlabyrinthe.mazes.*
import com.funlabyrinthe.mazes.std.*

enum Instruction derives Pickleable, Inspectable:
  case ShowMessage(
    message: String,
    onlyFirstTime: Boolean,
  )

  case ChangeSquare(
    map: Map,
    position: Position,
    field: Option[Field],
    effect: Option[Effect],
    tool: Option[Tool],
    obstacle: Option[Obstacle],
  )

  case ChangeButtonEnabled(button: PushButton, enabled: Boolean)

  case ChangePlayerColor(color: RGBA)

  case MovePlayer(map: Map, position: Position)

  case PlaySound(sound: Sound, volume: Volume, playbackPolicy: PlaybackPolicy)

  case Temporize

  case ContinueMove

  case ShowPlayer

  case HidePlayer

  case Win

  case Lose
end Instruction

object Instruction:
  def execute(instructions: List[Instruction], context: AbstractMoveContext, firstTime: Boolean)(using Universe): Unit =
    for instruction <- instructions do
      execute(instruction, context, firstTime)

  def execute(instruction: Instruction, context: AbstractMoveContext, firstTime: Boolean)(using Universe): Unit =
    import context.*

    instruction match
      case ShowMessage(message, onlyFirstTime) =>
        if !onlyFirstTime || firstTime then
          player.showMessage(message)

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

      case ChangePlayerColor(color) =>
        player.color = color

      case MovePlayer(map, position) =>
        player.position = Some(map.ref(position))

      case PlaySound(sound, volume, playbackPolicy) =>
        player.playSound(sound, volume, playbackPolicy)

      case Temporize =>
        temporize()

      case ContinueMove =>
        context match {
          case context: EnteredContext =>
            context.goOnMoving = true
          case context: ExecuteContext =>
            context.goOnMoving = true
          case _ =>
            // there's nothing we can do; ignore
            ()
        }

      case ShowPlayer =>
        player.show()

      case HidePlayer =>
        player.hide()

      case Win =>
        player.win()

      case Lose =>
        player.lose()
  end execute
