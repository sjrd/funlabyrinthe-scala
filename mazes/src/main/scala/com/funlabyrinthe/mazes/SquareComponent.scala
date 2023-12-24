package com.funlabyrinthe.mazes

import com.funlabyrinthe.core.*
import com.funlabyrinthe.core.graphics.DrawContext

abstract class SquareComponent(using ComponentInit) extends VisualComponent:
  final def drawTo(context: DrawSquareContext[Map]): Unit =
    doDraw(context)
    drawEditVisualTag(context)

  protected def doDraw(context: DrawSquareContext[Map]): Unit =
    super.doDraw(context)

  override protected final def doDraw(context: DrawContext): Unit =
    drawTo(new DrawSquareContext[Map](context))

  def dispatch[A]: PartialFunction[SquareMessage[A], A] = PartialFunction.empty
end SquareComponent
