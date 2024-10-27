package com.funlabyrinthe.mazes.std

import com.funlabyrinthe.core.*
import com.funlabyrinthe.mazes.*
import com.funlabyrinthe.core.graphics.DrawContext

class PlankOverridingField(using ComponentInit)(
  player: Player,
  pos: SquareRef,
  originalSquare: Square
) extends Field:
  override protected def doDraw(context: DrawSquareContext): Unit =
    originalSquare.drawTo(context)

  override def entering(context: MoveContext): Unit = {
    if context.player != player then
      context.cancel()
  }

  override def entered(context: MoveContext): Unit = {
    context.temporize()
    player.moveTo(pos +> player.direction.get, execute = true)
  }

  override def exited(context: MoveContext): Unit = {
    plankPlugin.inUse(player) = false
    pos() = originalSquare
  }
end PlankOverridingField

object PlankOverridingField:
  def install(player: Player, pos: SquareRef)(using ComponentInit): PlankOverridingField =
    val field = new PlankOverridingField(player, pos, pos())
    pos() = field
    field
  end install
end PlankOverridingField
