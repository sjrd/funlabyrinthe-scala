package com.funlabyrinthe.mazes.std

import scala.annotation.tailrec

import cps.customValueDiscard

import com.funlabyrinthe.core.*
import com.funlabyrinthe.mazes.*

sealed abstract class Turnstile(using ComponentInit) extends Effect {
  var pairingTurnstile: Turnstile = this

  def nextDirection(dir: Direction): Direction

  override def execute(context: MoveContext): Control[Unit] = control {
    import context._
    import player._

    if (!player.direction.isEmpty) {
      temporize()
      executeLoop(context, nextDirection(player.direction.get.opposite))
    }
  }

  private def executeLoop(context: MoveContext, dir: Direction): Control[Unit] = {
    val player = context.player
    val myPosition = context.pos

    // Unfortunate duplicate of Player.move()
    // But then ... turnstiles are deeply interacting, so it's expected
    if (player.playState == CorePlayer.PlayState.Playing) {
      val dest = player.position.get +> dir
      val nestedContext = new MoveContext(player, Some(dest), keyEvent = None)

      player.direction = Some(dir)
      player.testMoveAllowed(nestedContext).flatMap { moveAllowed =>
        if (moveAllowed) {
          if (player.position == nestedContext.src)
            player.moveTo(nestedContext, execute = true)
          else
            doNothing()
        } else {
          // blocked over there, loop to next direction
          if (player.position == Some(myPosition))
            executeLoop(context, nextDirection(dir))
          else
            doNothing()
        }
      }
    } else {
      doNothing()
    }
  }

  override def exited(context: MoveContext): Control[Unit] = control {
    context.pos() += pairingTurnstile
  }
}

class DirectTurnstile(using ComponentInit) extends Turnstile {
  painter += "Arrows/DirectTurnstile"

  override def nextDirection(dir: Direction) = dir.left
}

class IndirectTurnstile(using ComponentInit) extends Turnstile {
  painter += "Arrows/IndirectTurnstile"

  override def nextDirection(dir: Direction) = dir.right
}
