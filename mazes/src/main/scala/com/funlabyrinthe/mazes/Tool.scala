package com.funlabyrinthe.mazes

import com.funlabyrinthe.core.*

abstract class Tool(using ComponentInit) extends SquareComponent {
  category = ComponentCategory("tools", "Tools")

  def find(context: MoveContext): Unit = ()

  protected def editMapAdd(pos: SquareRef)(using EditingServices): Unit =
    pos() += this
    EditingServices.markModified()
  end editMapAdd

  protected def editMapRemove(pos: SquareRef)(using EditingServices): Unit =
    pos() += noTool
    EditingServices.markModified()
  end editMapRemove
}
