package com.funlabyrinthe.mazes

import com.funlabyrinthe.core.*
import com.funlabyrinthe.core.pickling.*

import com.funlabyrinthe.mazes.std.*

final case class Square(
    field: Field,
    effect: Effect,
    tool: Tool,
    obstacle: Obstacle
):
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
end Square

object Square:
  private val DefaultSquareIsPickleable: Pickleable[Square] =
    Pickleable.derived[Square]

  given SquarePickleable: Pickleable[Square] with
    def pickle(value: Square)(using PicklingContext): Pickle =
      DefaultSquareIsPickleable.pickle(value)

    def unpickle(pickle: Pickle)(using PicklingContext): Option[Square] =
      DefaultSquareIsPickleable.unpickle(pickle)

    def removeReferences(value: Square, reference: Component)(
        using PicklingContext): Pickleable.RemoveRefResult[Square] =
      given Universe = summon[PicklingContext].universe

      var changed = false

      def removeOne[C <: SquareComponent](part: C, default: C)(using Pickleable[C]): C =
        Pickleable.removeReferences(part, reference) match
          case Pickleable.RemoveRefResult.Unchanged =>
            part
          case Pickleable.RemoveRefResult.Changed(newValue) =>
            changed = true
            newValue
          case Pickleable.RemoveRefResult.Failure =>
            changed = true
            default

      val newField = removeOne(value.field, grass)
      val newEffect = removeOne(value.effect, noEffect)
      val newTool = removeOne(value.tool, noTool)
      val newObstacle = removeOne(value.obstacle, noObstacle)

      if changed then
        Pickleable.RemoveRefResult.Changed(Square(newField, newEffect, newTool, newObstacle))
      else
        Pickleable.RemoveRefResult.Unchanged
  end SquarePickleable
end Square
