package com.funlabyrinthe.mazes

import com.funlabyrinthe.core.*
import com.funlabyrinthe.core.graphics._

class DrawSquareContext(
  _gc: GraphicsContext,
  _rect: Rectangle2D,
  val where: Option[SquareRef],
  val purpose: DrawPurpose,
) extends DrawContext(_gc, _rect):
  def this(baseContext: DrawContext, where: Option[SquareRef], purpose: DrawPurpose) =
    this(baseContext.gc, baseContext.rect, where, purpose)

  def withGraphicsContext(gc: GraphicsContext, rect: Rectangle2D): DrawSquareContext =
    new DrawSquareContext(gc, rect, where, purpose)

  def withRect(rect: Rectangle2D): DrawSquareContext =
    new DrawSquareContext(gc, rect, where, purpose)

  def withWhere(where: Option[SquareRef]): DrawSquareContext =
    new DrawSquareContext(gc, rect, where, purpose)

  def withPurpose(purpose: DrawPurpose): DrawSquareContext =
    new DrawSquareContext(gc, rect, where, purpose)

  @inline final def isNowhere: Boolean = where.isEmpty
  @inline final def isSomewhere: Boolean = where.isDefined

  @inline final def map: Option[Map] =
    if (isNowhere) None else Some(where.get.map)

  @inline final def pos: Option[Position] =
    if (isNowhere) None else Some(where.get.pos)
end DrawSquareContext
