package com.funlabyrinthe.mazes

import com.funlabyrinthe.core.*

abstract class Field(using ComponentInit) extends SquareComponent {
  category = ComponentCategory("fields", "Fields")

  def entering(context: MoveContext): Control[Unit] = doNothing()
  def exiting(context: MoveContext): Control[Unit] = doNothing()

  def entered(context: MoveContext): Control[Unit] = doNothing()
  def exited(context: MoveContext): Control[Unit] = doNothing()

  protected def editMapAdd(pos: SquareRef[Map]): EditUserActionResult =
    if pos.isInside then
      pos() += this
    else
      pos.map.outside(pos.pos.z) += this
    EditUserActionResult.Done
  end editMapAdd

  protected def editMapRemove(pos: SquareRef[Map]): EditUserActionResult =
    // We never actually remove a field; it will get replaced instead
    EditUserActionResult.Done
  end editMapRemove
}
