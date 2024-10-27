package com.funlabyrinthe.mazes

import com.funlabyrinthe.core.*

abstract class Obstacle(using ComponentInit) extends SquareComponent derives Reflector {
  /** If true, the effect and tool on the same square are not drawn as long
   *  as this obstacle is there.
   *
   *  This can be used to hide them for gameplay reasons even though the
   *  obstacle's image is partially transparent.
   */
  var hideEffectAndTool: Boolean = false

  category = ComponentCategory("obstacles", "Obstacles")

  override def reflect() = autoReflect[Obstacle]

  def pushing(context: MoveContext): Unit = {
    context.cancel()
  }

  protected def editMapAdd(pos: SquareRef): EditUserActionResult =
    pos() += this
    EditUserActionResult.Done
  end editMapAdd

  protected def editMapRemove(pos: SquareRef): EditUserActionResult =
    pos() += noObstacle
    EditUserActionResult.Done
  end editMapRemove
}
