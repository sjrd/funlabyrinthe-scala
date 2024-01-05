package com.funlabyrinthe.mazes.std

import cps.customValueDiscard

import com.funlabyrinthe.core.*
import com.funlabyrinthe.mazes.*

sealed abstract class Stairs(using ComponentInit) extends Effect derives Reflector {
  var pairingStairs: Stairs = this

  override def reflect() = autoReflect[Stairs]

  def destinationOf(src: SquareRef[Map]): SquareRef[Map]

  override def execute(context: MoveContext) = control {
    import context._
    temporize()
    player.moveTo(destinationOf(pos), execute = false)
  }
}

class UpStairs(using ComponentInit) extends Stairs {
  name = "Up stairs"
  painter += "Stairs/UpStairs"

  override def destinationOf(src: SquareRef[Map]) = src + (0, 0, 1)
}

class DownStairs(using ComponentInit) extends Stairs {
  name = "Down stairs"
  painter += "Stairs/DownStairs"

  override def destinationOf(src: SquareRef[Map]) = src - (0, 0, 1)
}
