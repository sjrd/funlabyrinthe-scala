package com.funlabyrinthe.mazes

import com.funlabyrinthe.core.*
import com.funlabyrinthe.core.scene.*

abstract class Field(using ComponentInit) extends SquareComponent {
  category = ComponentCategory("fields", "Fields")

  final def presentCeiling(context: PresentSquareContext): Batch[SceneNode] =
    doPresentCeiling(context)

  protected def doPresentCeiling(context: PresentSquareContext): Batch[SceneNode] =
    Batch.empty

  override def presentIcon(): Batch[SceneNode] = {
    if isTemplate && templateIcon.items.nonEmpty then
      super.presentIcon()
    else
      val context = PresentSquareContext(tickCount = 0L, None, DrawPurpose.Icon(this), Size(30, 30))
      val base = present(context) ++ presentCeiling(context) ++ presentEditVisualTag()
      if isTemplate then
        base ++ universe.CreatorIconPainter.present()
      else
        base
  }

  @transient @noinspect
  final def toSquare: Square =
    Square(this, noEffect, noTool, noObstacle)

  final def +(effect: Effect): Square =
    toSquare + effect
  final def +(tool: Tool): Square =
    toSquare + tool
  final def +(obstacle: Obstacle): Square =
    toSquare + obstacle

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
