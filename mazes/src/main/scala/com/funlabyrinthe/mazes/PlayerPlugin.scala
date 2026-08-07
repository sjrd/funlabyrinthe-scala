package com.funlabyrinthe.mazes

import com.funlabyrinthe.core.*
import com.funlabyrinthe.core.input.*
import com.funlabyrinthe.core.scene.*

abstract class PlayerPlugin(using ComponentInit) extends CorePlayerPlugin:
  var painterUnder: Painter = universe.EmptyPainter
  var painterAbove: Painter = universe.EmptyPainter

  def presentUnder(player: Player, context: PresentSquareContext): Batch[SceneNode] =
    context.presentTiled(painterUnder)

  def presentAbove(player: Player, context: PresentSquareContext): Batch[SceneNode] =
    context.presentTiled(painterAbove)

  def moving(context: MoveContext): Unit = ()

  def moved(context: MoveContext): Unit = ()
end PlayerPlugin
