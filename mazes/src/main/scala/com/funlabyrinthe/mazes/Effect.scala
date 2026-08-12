package com.funlabyrinthe.mazes

import com.funlabyrinthe.core.*

abstract class Effect(using ComponentInit) extends SquareComponent {
  category = ComponentCategory("effects", "Effects")

  def entered(context: EnteredContext): Unit = ()
  def execute(context: ExecuteContext): Unit = ()
  def exited(context: ExitedContext): Unit = ()

  @transient @noinspect
  def isEmpty: Boolean = this.isInstanceOf[NoEffect]

  protected def editMapAdd(pos: SquareRef)(using EditingServices): Unit =
    pos() += this
    EditingServices.markModified()
  end editMapAdd

  protected def editMapRemove(pos: SquareRef)(using EditingServices): Unit =
    pos() += noEffect
    EditingServices.markModified()
  end editMapRemove
}
