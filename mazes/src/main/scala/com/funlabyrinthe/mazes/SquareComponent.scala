package com.funlabyrinthe.mazes

import com.funlabyrinthe.core.*
import com.funlabyrinthe.core.graphics.DrawContext

abstract class SquareComponent(using ComponentInit) extends VisualComponent:
  final def drawTo(context: DrawSquareContext): Unit =
    doDraw(context)
    drawEditVisualTag(context)

  protected def doDraw(context: DrawSquareContext): Unit =
    super.doDraw(context)

  override protected final def doDraw(context: DrawContext): Unit =
    drawTo(new DrawSquareContext(context))

  def dispatch[A]: PartialFunction[SquareMessage[A], A] = PartialFunction.empty

  protected def editMapAdd(pos: SquareRef): EditUserActionResult

  protected def editMapRemove(pos: SquareRef): EditUserActionResult

  private[mazes] final def editMapAddInternal(pos: SquareRef): EditUserActionResult =
    editMapAdd(pos)

  private[mazes] final def editMapRemoveInternal(pos: SquareRef): EditUserActionResult =
    editMapRemove(pos)
end SquareComponent
