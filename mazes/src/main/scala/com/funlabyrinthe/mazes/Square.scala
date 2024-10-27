package com.funlabyrinthe.mazes

import com.funlabyrinthe.core.*
import com.funlabyrinthe.mazes.std.*

import com.funlabyrinthe.core.pickling.Pickleable

final case class Square(
    field: Field,
    effect: Effect,
    tool: Tool,
    obstacle: Obstacle
) derives Pickleable {

  def drawTo(context: DrawSquareContext): Unit =
    field.drawTo(context)
    if !obstacle.hideEffectAndTool then
      effect.drawTo(context)
      tool.drawTo(context)
    obstacle.drawTo(context)
  end drawTo

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

  override def toString(): String = {
    given Universe = field.universe
    (field.toString +
        (if (effect != noEffect) ", " + effect.toString else "") +
        (if (tool != noTool) ", " + tool.toString else "") +
        (if (obstacle != noObstacle) ", " + obstacle.toString else ""))
  }

  protected def doEntering(context: MoveContext): Unit = {
    field.entering(context)
  }

  protected def doExiting(context: MoveContext): Unit = {
    field.exiting(context)
  }

  protected def doEntered(context: MoveContext): Unit = {
    field.entered(context)
    effect.entered(context)
  }

  protected def doExited(context: MoveContext): Unit = {
    field.exited(context)
    effect.exited(context)
  }

  protected def doExecute(context: MoveContext): Unit = {
    tool.find(context)
    effect.execute(context)
  }

  protected def doPushing(context: MoveContext): Unit = {
    obstacle.pushing(context)
  }

  private def hookEvent(
    context: MoveContext,
    hook: (PosComponent, MoveContext) => Unit
  ): Boolean = {
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

  def entering(context: MoveContext): Unit = {
    if !hookEvent(context, _.entering(_)) then
      doEntering(context)
  }

  def exiting(context: MoveContext): Unit = {
    if !hookEvent(context, _.exiting(_)) then
      doExiting(context)
  }

  def entered(context: MoveContext): Unit = {
    if !hookEvent(context, _.entered(_)) then
      doEntered(context)
  }

  def exited(context: MoveContext): Unit = {
    if !hookEvent(context, _.exited(_)) then
      doExited(context)
  }

  def execute(context: MoveContext): Unit = {
    if !hookEvent(context, _.execute(_)) then
      doExecute(context)
  }

  def pushing(context: MoveContext): Unit = {
    if !hookEvent(context, _.pushing(_)) then
      doPushing(context)
  }

  def dispatch[A](message: SquareMessage[A], pos: SquareRef): Option[A] =
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
