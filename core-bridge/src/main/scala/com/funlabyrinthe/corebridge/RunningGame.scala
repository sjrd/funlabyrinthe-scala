package com.funlabyrinthe.corebridge

import scala.scalajs.js
import scala.scalajs.js.JSConverters.*

import com.funlabyrinthe.core
import com.funlabyrinthe.coreinterface as intf

object RunningGame:
  def startGame(universe: core.Universe): RunningGame =
    Errors.protect {
      val players = universe.players.map(new Player(_)) // attaches controllers
      universe.startGame()
      players.foreach(_.processQueueItem()) // process items from startGame()
      RunningGame(universe, players.toJSArray)
    }
  end startGame
end RunningGame

final class RunningGame private (
  underlying: core.Universe,
  val players: js.Array[intf.Player],
) extends intf.RunningGame:
  def advanceTickCount(delta: Double): Unit =
    Errors.protect {
      underlying.advanceTickCount(delta.toLong)
    }
end RunningGame
