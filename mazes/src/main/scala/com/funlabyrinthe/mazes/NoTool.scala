package com.funlabyrinthe.mazes

import com.funlabyrinthe.core.*
import com.funlabyrinthe.core.graphics.*
import com.funlabyrinthe.core.scene.*

final class NoTool private[mazes] (using ComponentInit) extends Tool:
  override def drawIcon(context: DrawContext): Unit =
    universe.DefaultIconPainter.drawStretchedTo(context)

  override def presentIcon(): Batch[SceneNode] =
    universe.DefaultIconPainter.present()

  override protected def editMapRemove(pos: SquareRef)(using EditingServices): Unit =
    () // no change
end NoTool
