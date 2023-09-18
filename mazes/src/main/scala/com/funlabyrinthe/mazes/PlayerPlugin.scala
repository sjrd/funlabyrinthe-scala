package com.funlabyrinthe
package mazes

import core._
import input._

abstract class PlayerPlugin(using ComponentInit) extends CorePlayerPlugin:
  import universe.*

  var painterBefore: Painter = EmptyPainter
  var painterAfter: Painter = EmptyPainter

  def drawBefore(player: Player, context: DrawContext): Unit =
    painterBefore.drawTo(context)

  def drawAfter(player: Player, context: DrawContext): Unit =
    painterAfter.drawTo(context)

  def moving(context: MoveContext): Control[Unit] = doNothing()

  def moved(context: MoveContext): Control[Unit] = doNothing()
end PlayerPlugin
