package com.funlabyrinthe.mazes
package std

import cps.customValueDiscard

import com.funlabyrinthe.core._

import scala.annotation.tailrec

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
    import context._
    import player._

    // Unfortunate duplicate of Player.move()
    // But then ... turnstiles are deeply interacting, so it's expected
    if (playState == CorePlayer.PlayState.Playing) {
      val dest = position.get +> dir
      val context = new MoveContext(player, Some(dest), keyEvent)

      direction = Some(dir)
      testMoveAllowed(context).flatMap { moveAllowed =>
        if (moveAllowed) {
          if (position == context.src)
            moveTo(context)
          else
            doNothing()
        } else {
          // blocked over there, loop to next direction
          if (position == Some(pos))
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
  override def nextDirection(dir: Direction) = dir.left
}

class IndirectTurnstile(using ComponentInit) extends Turnstile {
  override def nextDirection(dir: Direction) = dir.right
}
