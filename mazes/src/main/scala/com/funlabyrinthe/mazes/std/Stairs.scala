package com.funlabyrinthe.mazes.std

import cps.customValueDiscard

import com.funlabyrinthe.core.*
import com.funlabyrinthe.mazes.*

sealed abstract class Stairs(using ComponentInit) extends Effect derives Reflector {
  var pairingStairs: Stairs = this

  override def reflect() = autoReflect[Stairs]

  def destinationOf(src: SquareRef): SquareRef

  override def execute(context: MoveContext) = control {
    import context._
    temporize()
    player.moveTo(destinationOf(pos), execute = false)
  }

  override protected def editMapAdd(pos: SquareRef): EditUserActionResult =
    val dest = destinationOf(pos)

    def doChange(): EditUserActionResult =
      pos() += this
      dest() += pairingStairs
      EditUserActionResult.Done

    if dest.isOutside then
      EditUserActionResult.Error("Cannot add stairs here because the destination is outside of the map")
    else if pos() == dest() then
      doChange()
    else
      EditUserActionResult.AskConfirmation(
        s"The destination square (${dest()}) is different from this square. "
          + "Are you sure you want to add stairs here?",
        onConfirm = () => doChange()
      )
  end editMapAdd

  override protected def editMapRemove(pos: SquareRef): EditUserActionResult =
    val dest = destinationOf(pos)

    if dest().effect == pairingStairs then
      dest() += noEffect

    super.editMapRemove(pos)
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
