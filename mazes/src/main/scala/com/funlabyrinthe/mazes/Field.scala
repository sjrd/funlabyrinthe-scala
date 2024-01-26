package com.funlabyrinthe.mazes

import com.funlabyrinthe.core.*
import com.funlabyrinthe.core.graphics.DrawContext

abstract class Field(using ComponentInit) extends SquareComponent {
  category = ComponentCategory("fields", "Fields")

  final def drawCeilingTo(context: DrawSquareContext[Map]): Unit =
    doDrawCeiling(context)

  protected def doDrawCeiling(context: DrawSquareContext[Map]): Unit = ()

  override def drawIcon(context: DrawContext): Unit =
    super.drawIcon(context)
    drawCeilingTo(DrawSquareContext(context, None))

  def entering(context: MoveContext): Control[Unit] = doNothing()
  def exiting(context: MoveContext): Control[Unit] = doNothing()

  def entered(context: MoveContext): Control[Unit] = doNothing()
  def exited(context: MoveContext): Control[Unit] = doNothing()

  protected def editMapAdd(pos: SquareRef[Map]): EditUserActionResult =
    if pos.isInside then
      pos() += this
    else
      pos.map.outside(pos.pos.z) += this
    EditUserActionResult.Done
  end editMapAdd

  protected def editMapRemove(pos: SquareRef[Map]): EditUserActionResult =
    // We never actually remove a field; it will get replaced instead
    EditUserActionResult.Done
  end editMapRemove

  protected def editMapRedirect(pos: SquareRef[Map], newComponent: SquareComponent): SquareRef[Map] =
    pos

  private[mazes] final def editMapRedirectInternal(pos: SquareRef[Map], newComponent: SquareComponent): SquareRef[Map] =
    editMapRedirect(pos, newComponent)
}
