package com.funlabyrinthe.mazes

import com.funlabyrinthe.core.*
import com.funlabyrinthe.core.graphics._

class DrawSquareContext(
    _gc: GraphicsContext, _rect: Rectangle2D,
    val where: Option[SquareRef[Map]]) extends DrawContext(_gc, _rect) {

  def this(baseContext: DrawContext, where: Option[SquareRef[Map]]) =
    this(baseContext.gc, baseContext.rect, where)

  def this(baseContext: DrawContext) =
    this(baseContext, None)

  @inline final def isNowhere = where.isEmpty
  @inline final def isSomewhere = where.isDefined

  @inline final def map: Option[Map] =
    if (isNowhere) None else Some(where.get.map)

  @inline final def pos: Option[Position] =
    if (isNowhere) None else Some(where.get.pos)
}
