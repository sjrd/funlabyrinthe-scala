package com.funlabyrinthe.mazes

import com.funlabyrinthe.core._

import scala.language.implicitConversions

trait Maps { self: MazePlugin =>
  import universe._

  type DrawSquareContext = universe.DrawSquareContext[Square]
  type Map = universe.Map[Square]
  type SquareRef = universe.SquareRef[Square]

  object SquareRef {
    @inline def apply(map: Map, pos: Position) = new SquareRef(map, pos)
    @inline def unapply(ref: SquareRef) = Some(ref.map, ref.pos)
  }

  class Square(
      val field: Field,
      val effect: Effect = NoEffect,
      val tool: Tool = NoTool,
      val obstacle: Obstacle = NoObstacle
  ) extends AbstractSquare[Square] {
    override def drawTo(context: DrawSquareContext) {
      for (part <- parts)
        part.drawTo(context)
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

    override def toString() = {
      (field.toString +
          (if (effect != NoEffect) "+" + effect.toString else "") +
          (if (tool != NoTool) "+" + tool.toString else "") +
          (if (obstacle != NoObstacle) "+" + obstacle.toString else ""))
    }

    protected def doEntering(context: MoveContext) {
      field.entering(context)
    }

    protected def doExiting(context: MoveContext) {
      field.exiting(context)
    }

    protected def doEntered(context: MoveContext) {
      field.entered(context)
      effect.entered(context)
    }

    protected def doExited(context: MoveContext) {
      field.exited(context)
      effect.exited(context)
    }

    protected def doExecute(context: MoveContext) {
      tool.find(context)
      effect.execute(context)
    }

    protected def doPushing(context: MoveContext) {
      obstacle.pushing(context)
    }

    def entering(context: MoveContext) {
      doEntering(context)
    }

    def exiting(context: MoveContext) {
      doExiting(context)
    }

    def entered(context: MoveContext) {
      doEntered(context)
    }

    def exited(context: MoveContext) {
      doExited(context)
    }

    def execute(context: MoveContext) {
      doExecute(context)
    }

    def pushing(context: MoveContext) {
      doPushing(context)
    }
  }

  implicit def fieldToSquare(field: Field): Square =
    new Square(field)

  class Field extends VisualComponent {
    def entering(context: MoveContext) {}
    def exiting(context: MoveContext) {}

    def entered(context: MoveContext) {}
    def exited(context: MoveContext) {}
  }

  class Effect extends VisualComponent {
    def entered(context: MoveContext) {}
    def exited(context: MoveContext) {}

    def execute(context: MoveContext) {}
  }

  class Tool extends VisualComponent {
    def find(context: MoveContext) {}
  }

  class Obstacle extends VisualComponent {
    def pushing(context: MoveContext) {}
  }

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
