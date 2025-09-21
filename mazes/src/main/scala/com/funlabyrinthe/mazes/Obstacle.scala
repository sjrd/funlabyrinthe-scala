package com.funlabyrinthe.mazes

import com.funlabyrinthe.core.*

abstract class Obstacle(using ComponentInit) extends SquareComponent {
  /** If true, the effect and tool on the same square are not drawn as long
   *  as this obstacle is there.
   *
   *  This can be used to hide them for gameplay reasons even though the
   *  obstacle's image is partially transparent.
   */
  var hideEffectAndTool: Boolean = false

  category = ComponentCategory("obstacles", "Obstacles")

  def pushing(context: MoveContext): Unit = {
    context.cancel()
  }

  protected def editMapAdd(pos: SquareRef)(using EditingServices): Unit =
    pos() += this
    EditingServices.markModified()
  end editMapAdd

  protected def editMapRemove(pos: SquareRef)(using EditingServices): Unit =
    pos() += noObstacle
    EditingServices.markModified()
  end editMapRemove
}
