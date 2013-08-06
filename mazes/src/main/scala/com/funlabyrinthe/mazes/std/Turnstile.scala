package com.funlabyrinthe.mazes
package std

import com.funlabyrinthe.core._

import scala.annotation.tailrec

trait Turnstile extends Effect {
  var pairingTurnstile: Turnstile = this

  def nextDirection(dir: Direction): Direction

  override def execute(context: MoveContext): Unit = {
    import context._

    if (player.direction.isEmpty)
      return

    temporize()

    @tailrec
    def loop(dir: Direction): Option[Player.MoveTrampoline] = {
      val trampoline = player.move(dir, None)
      if (player.position == Some(pos))
        loop(nextDirection(dir))
      else
        trampoline
    }

    for (trampoline <- loop(nextDirection(player.direction.get.opposite))) {
      temporization = trampoline.delay
      goOnMoving = true
    }
  }

  override def exited(context: MoveContext) = {
    context.pos() += pairingTurnstile
  }
}

trait DirectTurnstile extends Turnstile {
  override def nextDirection(dir: Direction) = dir.left
}

trait IndirectTurnstile extends Turnstile {
  override def nextDirection(dir: Direction) = dir.right
}
