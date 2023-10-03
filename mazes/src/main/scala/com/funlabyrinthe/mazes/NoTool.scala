package com.funlabyrinthe.mazes

import com.funlabyrinthe.core.*

final class NoTool private[mazes] (using ComponentInit) extends Tool:
  import universe.*

  name = "(no tool)"

  override def drawIcon(context: DrawContext): Unit =
    DefaultIconPainter.drawTo(context)
end NoTool
