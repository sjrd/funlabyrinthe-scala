package com.funlabyrinthe.mazes

import com.funlabyrinthe.core.*
import com.funlabyrinthe.core.graphics._
import indigo.Batch
import indigo.SceneNode

class PresentSquareContext(
  val tickCount: Long,
  val where: Option[SquareRef],
  val purpose: DrawPurpose,
):
  def withWhere(where: Option[SquareRef]): PresentSquareContext =
    new PresentSquareContext(tickCount, where, purpose)

  def withPurpose(purpose: DrawPurpose): PresentSquareContext =
    new PresentSquareContext(tickCount, where, purpose)

  @inline final def isNowhere: Boolean = where.isEmpty
  @inline final def isSomewhere: Boolean = where.isDefined

  @inline final def map: Option[Map] =
    if (isNowhere) None else Some(where.get.map)

  @inline final def pos: Option[Position] =
    if (isNowhere) None else Some(where.get.pos)

  def presentTiled(painter: Painter): Batch[SceneNode] =
    where match
      case Some(w) => painter.presentTiled(w.pos.x, w.pos.y)
      case None    => painter.presentTiled(0, 0)
end PresentSquareContext
