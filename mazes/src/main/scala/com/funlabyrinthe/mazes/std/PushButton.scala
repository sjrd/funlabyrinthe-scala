package com.funlabyrinthe.mazes.std

import com.funlabyrinthe.core.*
import com.funlabyrinthe.core.scene.*
import com.funlabyrinthe.mazes.*

class PushButton(using ComponentInit) extends Effect:
  painter += "Buttons/Button"
  var downPainter: Painter = universe.EmptyPainter + "Buttons/SunkenButton"
  var enabled: Boolean = true

  override protected def doPresent(context: PresentSquareContext): Batch[SceneNode] = {
    if enabled && !context.where.exists(pos => pos.map.playersBottomUp(pos.pos).nonEmpty) then
      doPresentUp(context)
    else
      doPresentDown(context)
  }

  protected def doPresentUp(context: PresentSquareContext): Batch[SceneNode] =
    context.presentTiled(painter)

  protected def doPresentDown(context: PresentSquareContext): Batch[SceneNode] =
    context.presentTiled(downPainter)

  override def execute(context: MoveContext): Unit = ()

  override def entered(context: MoveContext): Unit = {
    super.entered(context)

    if enabled then
      super.execute(context)
      if context.pos.map.playersBottomUp(context.pos.pos).sizeIs == 1 then
        buttonDown(context)
  }

  override def exited(context: MoveContext): Unit = {
    if enabled && context.pos.map.playersBottomUp(context.pos.pos).isEmpty then
      buttonUp(context)

    super.exited(context)
  }

  /** Executed when the button is pushed down. */
  def buttonDown(context: MoveContext): Unit = ()

  /** Executed when the button is released. */
  def buttonUp(context: MoveContext): Unit = ()
end PushButton
