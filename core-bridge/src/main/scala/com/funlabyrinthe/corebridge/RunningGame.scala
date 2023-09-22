package com.funlabyrinthe.corebridge

import scala.scalajs.js.JSConverters.*

import com.funlabyrinthe.core
import com.funlabyrinthe.coreinterface as intf

final class RunningGame(underlying: core.Universe) extends intf.RunningGame:
  val players = underlying.players.toJSArray.map(new Player(_))

  def advanceTickCount(delta: Double): Unit =
    underlying.advanceTickCount(delta.toLong)
end RunningGame
