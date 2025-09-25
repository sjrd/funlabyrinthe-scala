package com.funlabyrinthe.mazes

import com.funlabyrinthe.core.*
import com.funlabyrinthe.core.graphics.*
import com.funlabyrinthe.core.input.*

abstract class PlayerPlugin(using ComponentInit) extends CorePlayerPlugin:
  var painterBefore: Painter = universe.EmptyPainter
  var painterAfter: Painter = universe.EmptyPainter

  def drawBefore(player: Player, context: DrawSquareContext): Unit =
    context.drawTiled(painterBefore)

  def drawAfter(player: Player, context: DrawSquareContext): Unit =
    context.drawTiled(painterAfter)

  def moving(context: MoveContext): Unit = ()

  def moved(context: MoveContext): Unit = ()
end PlayerPlugin
