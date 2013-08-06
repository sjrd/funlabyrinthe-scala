package com.funlabyrinthe.mazes
package std

import com.funlabyrinthe.core._

trait Stairs extends Effect {
  import universe._
  import mazes._

  var pairingStairs: Stairs = this

  def destinationOf(src: SquareRef[Map]): SquareRef[Map]

  override def execute(context: MoveContext) {
    import context._
    temporize()
    player.moveTo(destinationOf(pos), execute = false)
  }
}

trait UpStairs extends Stairs {
  override def destinationOf(src: SquareRef[Map]) = src + (0, 0, 1)
}

trait DownStairs extends Stairs {
  override def destinationOf(src: SquareRef[Map]) = src - (0, 0, 1)
}
