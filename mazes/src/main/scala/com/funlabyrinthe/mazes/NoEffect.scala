package com.funlabyrinthe.mazes

import com.funlabyrinthe.core.*
import com.funlabyrinthe.core.graphics.*

final class NoEffect private[mazes] (using ComponentInit) extends Effect:
  override def drawIcon(context: DrawContext): Unit =
    universe.DefaultIconPainter.drawStretchedTo(context)

  override protected def editMapRemove(pos: SquareRef)(using EditingServices): Unit =
    () // no change
end NoEffect
