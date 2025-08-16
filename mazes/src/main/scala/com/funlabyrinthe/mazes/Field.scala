package com.funlabyrinthe.mazes

import com.funlabyrinthe.core.*
import com.funlabyrinthe.core.graphics.DrawContext

abstract class Field(using ComponentInit) extends SquareComponent {
  category = ComponentCategory("fields", "Fields")

  final def drawCeilingTo(context: DrawSquareContext): Unit =
    doDrawCeiling(context)

  protected def doDrawCeiling(context: DrawSquareContext): Unit = ()

  override def drawIcon(context: DrawContext): Unit =
    super.drawIcon(context)
    drawCeilingTo(DrawSquareContext(context, None, DrawPurpose.Icon(this)))

  def entering(context: MoveContext): Unit = ()
  def exiting(context: MoveContext): Unit = ()

  def entered(context: MoveContext): Unit = ()
  def exited(context: MoveContext): Unit = ()

  protected def editMapAdd(pos: SquareRef)(using EditingServices): Unit =
    if pos.isInside then
      pos() += this
    else
      pos.map.outside(pos.pos.z) += this
    EditingServices.markModified()
  end editMapAdd

  protected def editMapRemove(pos: SquareRef)(using EditingServices): Unit =
    // We never actually remove a field; it will get replaced instead
    ()
  end editMapRemove

  protected def editMapRedirect(pos: SquareRef, newComponent: SquareComponent): SquareRef =
    pos

  private[mazes] final def editMapRedirectInternal(pos: SquareRef, newComponent: SquareComponent): SquareRef =
    editMapRedirect(pos, newComponent)
}
