package com.funlabyrinthe.mazes

import com.funlabyrinthe.core.*

final class NoEffect private[mazes] (using ComponentInit) extends Effect:
  import universe.*

  name = "(no effect)"

  override def drawIcon(context: DrawContext): Unit =
    DefaultIconPainter.drawTo(context)

  override protected def editMapRemove(pos: SquareRef[Map]): EditUserActionResult =
    EditUserActionResult.Unchanged
end NoEffect
