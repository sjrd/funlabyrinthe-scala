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

  def entering(context: MoveContext): Control[Unit] = doNothing()
  def exiting(context: MoveContext): Control[Unit] = doNothing()

  def entered(context: MoveContext): Control[Unit] = doNothing()
  def exited(context: MoveContext): Control[Unit] = doNothing()

  protected def editMapAdd(pos: SquareRef): EditUserActionResult =
    if pos.isInside then
      pos() += this
    else
      pos.map.outside(pos.pos.z) += this
    EditUserActionResult.Done
  end editMapAdd

  protected def editMapRemove(pos: SquareRef): EditUserActionResult =
    // We never actually remove a field; it will get replaced instead
    EditUserActionResult.Done
  end editMapRemove

  protected def editMapRedirect(pos: SquareRef, newComponent: SquareComponent): SquareRef =
    pos

  private[mazes] final def editMapRedirectInternal(pos: SquareRef, newComponent: SquareComponent): SquareRef =
    editMapRedirect(pos, newComponent)
}
