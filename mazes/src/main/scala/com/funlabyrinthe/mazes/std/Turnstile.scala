package com.funlabyrinthe.mazes.std

import scala.annotation.tailrec

import com.funlabyrinthe.core.*
import com.funlabyrinthe.mazes.*

sealed abstract class Turnstile(using ComponentInit) extends Effect {
  var pairingTurnstile: Turnstile = this

  def nextDirection(dir: Direction): Direction

  override def execute(context: ExecuteContext): Unit = {
    import context.*

    temporize()
    executeLoop(context, nextDirection(player.direction.opposite))
  }

  @tailrec
  private def executeLoop(context: ExecuteContext, dir: Direction): Unit = {
    val player = context.player
    val myPosition = context.pos

    // Unfortunate duplicate of Player.move()
    // But then ... turnstiles are deeply interacting, so it's expected
    if (player.playState == CorePlayer.PlayState.Playing) {
      val previousDirection = dir // rationale: the turnstile turns the player before pushing them out
      player.direction = dir

      if player.testMoveAllowed(previousDirection, keyEvent = None) then
        player.moveTo(myPosition +> dir, execute = true)
      else
        // blocked over there, loop to next direction
        if player.position.contains(myPosition) then
          executeLoop(context, nextDirection(dir))
    }
  }

  override def exited(context: ExitedContext): Unit = {
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
