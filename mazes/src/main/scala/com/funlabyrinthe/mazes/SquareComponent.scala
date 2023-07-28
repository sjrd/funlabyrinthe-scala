package com.funlabyrinthe.mazes

import com.funlabyrinthe.core.*
import com.funlabyrinthe.core.graphics.DrawContext

abstract class SquareComponent(using ComponentInit) extends VisualComponent:
  def drawTo(context: DrawSquareContext[Map]): Unit =
    super.drawTo(context)

  override final def drawTo(context: DrawContext): Unit =
    drawTo(new DrawSquareContext[Map](context))

  def dispatch[A]: PartialFunction[SquareMessage[A], A] = PartialFunction.empty
end SquareComponent
