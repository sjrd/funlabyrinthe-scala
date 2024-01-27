package com.funlabyrinthe.mazes

import com.funlabyrinthe.core.*
import com.funlabyrinthe.core.graphics.*
import com.funlabyrinthe.core.input.*

abstract class PlayerPlugin(using ComponentInit) extends CorePlayerPlugin derives Reflector:
  var painterBefore: Painter = universe.EmptyPainter
  var painterAfter: Painter = universe.EmptyPainter

  override def reflect() = autoReflect[PlayerPlugin]

  def drawBefore(player: Player, context: DrawContext): Unit =
    painterBefore.drawTo(context)

  def drawAfter(player: Player, context: DrawContext): Unit =
    painterAfter.drawTo(context)

  def moving(context: MoveContext): Control[Unit] = doNothing()

  def moved(context: MoveContext): Control[Unit] = doNothing()
end PlayerPlugin
