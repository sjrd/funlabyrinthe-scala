package com.funlabyrinthe.mazes

import com.funlabyrinthe.core.*
import com.funlabyrinthe.core.input.*

abstract class PlayerPlugin(using ComponentInit) extends CorePlayerPlugin derives Reflector:
  import universe.*

  var painterBefore: Painter = EmptyPainter
  var painterAfter: Painter = EmptyPainter

  override def reflect() = autoReflect[PlayerPlugin]

  def drawBefore(player: Player, context: DrawContext): Unit =
    painterBefore.drawTo(context)

  def drawAfter(player: Player, context: DrawContext): Unit =
    painterAfter.drawTo(context)

  def moving(context: MoveContext): Control[Unit] = doNothing()

  def moved(context: MoveContext): Control[Unit] = doNothing()
end PlayerPlugin
