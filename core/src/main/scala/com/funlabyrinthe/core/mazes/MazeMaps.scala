package com.funlabyrinthe.core
package mazes

import scala.language.implicitConversions

trait MazeMaps { universe: MazeUniverse =>
  class MazeMap(_dimensions: Dimensions) extends Map(_dimensions) {
    type Square = MazeSquare
  }

  class MazeSquare(
      val field: MazeField,
      val effect: MazeEffect = NoMazeEffect,
      val tool: MazeTool = NoMazeTool,
      val obstacle: MazeObstacle = NoMazeObstacle
  ) extends AbstractSquare {
    final def +(field: MazeField) =
      new MazeSquare(field, effect, tool, obstacle)
    final def +(effect: MazeEffect) =
      new MazeSquare(field, effect, tool, obstacle)
    final def +(tool: MazeTool) =
      new MazeSquare(field, effect, tool, obstacle)
    final def +(obstacle: MazeObstacle) =
      new MazeSquare(field, effect, tool, obstacle)
  }

  implicit def mazeFieldToSquare(field: MazeField): MazeSquare =
    new MazeSquare(field)

  class MazeField extends VisualComponent
  class MazeEffect extends VisualComponent
  class MazeTool extends VisualComponent
  class MazeObstacle extends VisualComponent

  object NoMazeEffect extends MazeEffect
  object NoMazeTool extends MazeTool
  object NoMazeObstacle extends MazeObstacle
}
