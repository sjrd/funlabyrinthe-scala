package com.funlabyrinthe.mazes

import com.funlabyrinthe.core.*

final class NoTool private[mazes] (using ComponentInit) extends Tool:
  import universe.*

  override def drawIcon(context: DrawContext): Unit =
    DefaultIconPainter.drawTo(context)

  override protected def editMapRemove(pos: SquareRef): EditUserActionResult =
    EditUserActionResult.Unchanged
end NoTool
