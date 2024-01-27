package com.funlabyrinthe.mazes

import com.funlabyrinthe.core.*
import com.funlabyrinthe.core.graphics.*

abstract class SquareComponent(using ComponentInit) extends Component derives Reflector:
  var painter: Painter = universe.EmptyPainter

  override def reflect() = autoReflect[SquareComponent]

  final def drawTo(context: DrawSquareContext): Unit =
    doDraw(context)
    drawEditVisualTag(context)

  protected def doDraw(context: DrawSquareContext): Unit =
    painter.drawTo(context)

  override def drawIcon(context: DrawContext): Unit =
    drawTo(DrawSquareContext(context, None, DrawPurpose.Icon(this)))

  def dispatch[A]: PartialFunction[SquareMessage[A], A] = PartialFunction.empty

  protected def editMapAdd(pos: SquareRef): EditUserActionResult

  protected def editMapRemove(pos: SquareRef): EditUserActionResult

  private[mazes] final def editMapAddInternal(pos: SquareRef): EditUserActionResult =
    editMapAdd(pos)

  private[mazes] final def editMapRemoveInternal(pos: SquareRef): EditUserActionResult =
    editMapRemove(pos)
end SquareComponent
