package com.funlabyrinthe.mazes

import com.funlabyrinthe.core.*

abstract class Tool(using ComponentInit) extends SquareComponent {
  category = ComponentCategory("tools", "Tools")

  def find(context: MoveContext): Control[Unit] = doNothing()

  protected def editMapAdd(pos: SquareRef[Map]): EditUserActionResult =
    pos() += this
    EditUserActionResult.Done
  end editMapAdd

  protected def editMapRemove(pos: SquareRef[Map]): EditUserActionResult =
    pos() += noTool
    EditUserActionResult.Done
  end editMapRemove
}
