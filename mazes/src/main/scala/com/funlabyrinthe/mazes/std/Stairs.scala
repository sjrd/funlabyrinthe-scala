package com.funlabyrinthe.mazes
package std

import com.funlabyrinthe.core._

sealed abstract class Stairs(using ComponentInit) extends Effect derives Reflector {
  var pairingStairs: Stairs = this

  def destinationOf(src: SquareRef[Map]): SquareRef[Map]

  override def execute(context: MoveContext) = {
    import context._
    temporize()
    player.moveTo(destinationOf(pos), execute = false)
  }
}

class UpStairs(using ComponentInit) extends Stairs {
  override def destinationOf(src: SquareRef[Map]) = src + (0, 0, 1)
}

class DownStairs(using ComponentInit) extends Stairs {
  override def destinationOf(src: SquareRef[Map]) = src - (0, 0, 1)
}
