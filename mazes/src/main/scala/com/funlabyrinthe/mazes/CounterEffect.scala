package com.funlabyrinthe.mazes

import com.funlabyrinthe.core.*
import com.funlabyrinthe.mazes.*

/** Base class for effects that remember how many times they have been executed. */
abstract class CounterEffect(using ComponentInit) extends Effect derives Reflector:
  var globalCounter: Int = 0

  @transient @noinspect // FIXME We actually need to persist and inspect this
  object counter extends CorePlayer.mutable.SimplePerPlayerData[Int](0)

  override def reflect() = autoReflect[CounterEffect]

  override def execute(context: MoveContext): Unit =
    counter(context.player) += 1
    globalCounter += 1
  end execute

  def isFirstTime(player: Player): Boolean =
    counter(player) == 1
end CounterEffect
