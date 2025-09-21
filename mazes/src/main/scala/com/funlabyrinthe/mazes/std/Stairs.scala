package com.funlabyrinthe.mazes.std

import com.funlabyrinthe.core.*
import com.funlabyrinthe.mazes.*

sealed abstract class Stairs(using ComponentInit) extends Effect {
  var pairingStairs: Stairs = this

  def destinationOf(src: SquareRef): SquareRef

  override def execute(context: MoveContext): Unit = {
    import context._
    temporize()
    player.moveTo(destinationOf(pos), execute = false)
  }

  override protected def editMapAdd(pos: SquareRef)(using EditingServices): Unit =
    val dest = destinationOf(pos)

    if dest.isOutside then
      EditingServices.error("Cannot add stairs here because the destination is outside of the map")

    if pos() != dest() then
      EditingServices.askConfirmationOrCancel(
        s"The destination square (${dest()}) is different from this square. "
          + "Are you sure you want to add stairs here?"
      )

    pos() += this
    dest() += pairingStairs
    EditingServices.markModified()
  end editMapAdd

  override protected def editMapRemove(pos: SquareRef)(using EditingServices): Unit =
    val dest = destinationOf(pos)

    if dest().effect == pairingStairs then
      dest() += noEffect

    super.editMapRemove(pos) // will call markModified()
  end editMapRemove
}

class UpStairs(using ComponentInit) extends Stairs {
  painter += "Stairs/UpStairs"

  override def destinationOf(src: SquareRef) = src + (0, 0, 1)
}

class DownStairs(using ComponentInit) extends Stairs {
  painter += "Stairs/DownStairs"

  override def destinationOf(src: SquareRef) = src - (0, 0, 1)
}
