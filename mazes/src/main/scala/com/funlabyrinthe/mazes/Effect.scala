package com.funlabyrinthe.mazes

import com.funlabyrinthe.core.*

abstract class Effect(using ComponentInit) extends SquareComponent {
  category = ComponentCategory("effects", "Effects")

  def entered(context: MoveContext): Unit = ()
  def exited(context: MoveContext): Unit = ()

  def execute(context: MoveContext): Unit = ()

  protected def editMapAdd(pos: SquareRef)(using EditingServices): Unit =
    pos() += this
    EditingServices.markModified()
  end editMapAdd

  protected def editMapRemove(pos: SquareRef)(using EditingServices): Unit =
    pos() += noEffect
    EditingServices.markModified()
  end editMapRemove
}
