package com.funlabyrinthe.mazes

import com.funlabyrinthe.core.*

abstract class Effect(using ComponentInit) extends SquareComponent {
  category = ComponentCategory("effects", "Effects")

  def entered(context: MoveContext): Unit = ()
  def exited(context: MoveContext): Unit = ()

  def execute(context: MoveContext): Unit = ()

  protected def editMapAdd(pos: SquareRef): EditUserActionResult =
    pos() += this
    EditUserActionResult.Done
  end editMapAdd

  protected def editMapRemove(pos: SquareRef): EditUserActionResult =
    pos() += noEffect
    EditUserActionResult.Done
  end editMapRemove
}
