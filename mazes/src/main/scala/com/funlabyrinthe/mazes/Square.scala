package com.funlabyrinthe.mazes

import cps.customValueDiscard

import com.funlabyrinthe.core.*
import com.funlabyrinthe.mazes.std.*

import com.funlabyrinthe.core.pickling.Pickleable

final case class Square(
    field: Field,
    effect: Effect,
    tool: Tool,
    obstacle: Obstacle
) extends AbstractSquare[Square] derives Pickleable {

  type Map = com.funlabyrinthe.mazes.Map

  def drawTo(context: DrawSquareContext): Unit = {
    for (part <- parts)
      part.drawTo(context)
  }

  final def drawCeilingTo(context: DrawSquareContext): Unit =
    field.drawCeilingTo(context)

  final def parts: List[SquareComponent] = List(field, effect, tool, obstacle)

  final def +(field: Field) =
    new Square(field, effect, tool, obstacle)
  final def +(effect: Effect) =
    new Square(field, effect, tool, obstacle)
  final def +(tool: Tool) =
    new Square(field, effect, tool, obstacle)
  final def +(obstacle: Obstacle) =
    new Square(field, effect, tool, obstacle)

  override def toString() = {
    given Universe = field.universe
    (field.toString +
        (if (effect != noEffect) "+" + effect.toString else "") +
        (if (tool != noTool) "+" + tool.toString else "") +
        (if (obstacle != noObstacle) "+" + obstacle.toString else ""))
  }

  protected def doEntering(context: MoveContext): Control[Unit] = control {
    field.entering(context)
  }

  protected def doExiting(context: MoveContext): Control[Unit] = control {
    field.exiting(context)
  }

  protected def doEntered(context: MoveContext): Control[Unit] = control {
    field.entered(context)
    effect.entered(context)
  }

  protected def doExited(context: MoveContext): Control[Unit] = control {
    field.exited(context)
    effect.exited(context)
  }

  protected def doExecute(context: MoveContext): Control[Unit] = control {
    tool.find(context)
    effect.execute(context)
  }

  protected def doPushing(context: MoveContext): Control[Unit] = control {
    obstacle.pushing(context)
  }

  private def hookEvent(
    context: MoveContext,
    hook: (PosComponent, MoveContext) => Control[Unit]
  ): Control[Boolean] = control {
    var xs = context.pos.map.posComponentsTopDown(context.pos.pos)
    while !context.hooked && xs.nonEmpty do
      hook(xs.head, context)
      xs = xs.tail

    if context.hooked then
      context.hooked = false
      true
    else
      false
  }

  def entering(context: MoveContext): Control[Unit] = control {
    if !exec(hookEvent(context, _.entering(_))) then
      doEntering(context)
  }

  def exiting(context: MoveContext): Control[Unit] = control {
    if !exec(hookEvent(context, _.exiting(_))) then
      doExiting(context)
  }

  def entered(context: MoveContext): Control[Unit] = control {
    if !exec(hookEvent(context, _.entered(_))) then
      doEntered(context)
  }

  def exited(context: MoveContext): Control[Unit] = control {
    if !exec(hookEvent(context, _.exited(_))) then
      doExited(context)
  }

  def execute(context: MoveContext): Control[Unit] = control {
    if !exec(hookEvent(context, _.execute(_))) then
      doExecute(context)
  }

  def pushing(context: MoveContext): Control[Unit] = control {
    if !exec(hookEvent(context, _.pushing(_))) then
      doPushing(context)
  }

  def dispatch[A](message: SquareMessage[A], pos: SquareRef[Map]): Option[A] =
    var xs = pos.map.posComponentsTopDown(pos.pos)
    var result: Option[A] = None
    while result.isEmpty && xs.nonEmpty do
      result = xs.head.dispatch[A].lift(message)
      xs = xs.tail

    result
      .orElse(field.dispatch[A].lift(message))
      .orElse(effect.dispatch[A].lift(message))
      .orElse(tool.dispatch[A].lift(message))
      .orElse(obstacle.dispatch[A].lift(message))
  end dispatch
}
