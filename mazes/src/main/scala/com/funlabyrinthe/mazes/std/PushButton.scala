package com.funlabyrinthe.mazes.std

import cps.customValueDiscard

import com.funlabyrinthe.core.*
import com.funlabyrinthe.core.graphics.*
import com.funlabyrinthe.mazes.*

class PushButton(using ComponentInit) extends CounterEffect derives Reflector:
  painter += "Buttons/Button"
  var downPainter: Painter = universe.EmptyPainter + "Buttons/SunkenButton"
  var enabled: Boolean = true

  override def reflect() = autoReflect[PushButton]

  override protected def doDraw(context: DrawSquareContext[Map]): Unit =
    if enabled && !context.where.exists(pos => pos.map.playersBottomUp(pos.pos).nonEmpty) then
      doDrawUp(context)
    else
      doDrawDown(context)
  end doDraw

  protected def doDrawUp(context: DrawSquareContext[Map]): Unit =
    painter.drawTo(context)

  protected def doDrawDown(context: DrawSquareContext[Map]): Unit =
    downPainter.drawTo(context)

  override def execute(context: MoveContext): Control[Unit] = doNothing()

  override def entered(context: MoveContext): Control[Unit] = control {
    super.entered(context)

    if enabled then
      super.execute(context)
      if context.pos.map.playersBottomUp(context.pos.pos).sizeIs == 1 then
        buttonDown(context)
  }

  override def exited(context: MoveContext): Control[Unit] = control {
    if enabled && context.pos.map.playersBottomUp(context.pos.pos).isEmpty then
      buttonUp(context)

    super.exited(context)
  }

  /** Executed when the button is pushed down. */
  def buttonDown(context: MoveContext): Control[Unit] = doNothing()

  /** Executed when the button is released. */
  def buttonUp(context: MoveContext): Control[Unit] = doNothing()
end PushButton
