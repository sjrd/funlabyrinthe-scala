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

      def loop(dir: Direction): Control[Unit] = control {
        // Unfortunate duplicate of Player.move()
        // But then ... turnstiles are deeply interacting, so it's expected
        if (playState == Player.PlayState.Playing) {
          val dest = position.get +> dir
          val context = new MoveContext(player, Some(dest), keyEvent)

          direction = Some(dir)
          if (exec(testMoveAllowed(context))) {
            if (position == context.src)
              moveTo(context)
          } else {
            // blocked over there, loop to next direction
            if (position == Some(pos))
              loop(nextDirection(dir))
          }
        }
      }

      loop(nextDirection(player.direction.get.opposite))
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
