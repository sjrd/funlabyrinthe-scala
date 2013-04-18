package com.funlabyrinthe.core

import graphics._

class DrawSquareContext[M <: SquareMap](
    _gc: GraphicsContext, _rect: Rectangle2D,
    val where: Option[SquareRef[M]]) extends DrawContext(_gc, _rect) {

  type Map = M

  def this(baseContext: DrawContext) =
    this(baseContext.gc, baseContext.rect, None)

  @inline final def isNowhere = where.isEmpty
  @inline final def isSomewhere = where.isDefined

  @inline final def map: Option[Map] =
    if (isNowhere) None else Some(where.get.map)

  @inline final def pos: Option[Position] =
    if (isNowhere) None else Some(where.get.pos)
}
