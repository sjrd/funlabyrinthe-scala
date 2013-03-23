package com.funlabyrinthe.mazes

import com.funlabyrinthe.core._

import scala.language.implicitConversions

trait Maps { self: MazePlugin =>
  import universe._

  class Map(_dimensions: Dimensions) extends universe.Map(_dimensions) {
    type Square = self.Square
  }

  class Square(
      val field: Field,
      val effect: Effect = NoEffect,
      val tool: Tool = NoTool,
      val obstacle: Obstacle = NoObstacle
  ) extends AbstractSquare {
    override def drawTo(context: graphics.GraphicsContext,
        x: Double, y: Double) {
      for (part <- parts)
        part.drawTo(context, x, y)
    }

    final protected def parts = Seq(field, effect, tool, obstacle)

    final def +(field: Field) =
      new Square(field, effect, tool, obstacle)
    final def +(effect: Effect) =
      new Square(field, effect, tool, obstacle)
    final def +(tool: Tool) =
      new Square(field, effect, tool, obstacle)
    final def +(obstacle: Obstacle) =
      new Square(field, effect, tool, obstacle)
  }

  implicit def fieldToSquare(field: Field): Square =
    new Square(field)

  class Field extends VisualComponent
  class Effect extends VisualComponent
  class Tool extends VisualComponent
  class Obstacle extends VisualComponent

  object NoEffect extends Effect
  object NoTool extends Tool
  object NoObstacle extends Obstacle

  class Ground extends Field {
    def this(painter: Painter) {
      this()
      this.painter = painter
    }
  }

  object Grass extends Ground("Fields/Grass")
}
