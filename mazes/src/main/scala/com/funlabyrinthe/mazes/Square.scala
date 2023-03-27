package com.funlabyrinthe
package mazes

import core._

case class Square(
    field: Field,
    effect: Effect,
    tool: Tool,
    obstacle: Obstacle
) extends AbstractSquare[Square] {

  type Map = mazes.Map

  override def drawTo(context: DrawSquareContext[Map]): Unit = {
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
    import field.universe.mazes._
    (field.toString +
        (if (effect != NoEffect) "+" + effect.toString else "") +
        (if (tool != NoTool) "+" + tool.toString else "") +
        (if (obstacle != NoObstacle) "+" + obstacle.toString else ""))
  }

  protected def doEntering(context: MoveContext): Unit @control = {
    field.entering(context)
  }

  protected def doExiting(context: MoveContext): Unit @control = {
    field.exiting(context)
  }

  protected def doEntered(context: MoveContext): Unit @control = {
    field.entered(context)
    effect.entered(context)
  }

  protected def doExited(context: MoveContext): Unit @control = {
    field.exited(context)
    effect.exited(context)
  }

  protected def doExecute(context: MoveContext): Unit @control = {
    tool.find(context)
    effect.execute(context)
  }

  protected def doPushing(context: MoveContext): Unit @control = {
    obstacle.pushing(context)
  }

  def entering(context: MoveContext): Unit @control = {
    doEntering(context)
  }

  def exiting(context: MoveContext): Unit @control = {
    doExiting(context)
  }

  def entered(context: MoveContext): Unit @control = {
    doEntered(context)
  }

  def exited(context: MoveContext): Unit @control = {
    doExited(context)
  }

  def execute(context: MoveContext): Unit @control = {
    doExecute(context)
  }

  def pushing(context: MoveContext): Unit @control = {
    doPushing(context)
  }
}
