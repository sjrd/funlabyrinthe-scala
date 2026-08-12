package com.funlabyrinthe.mazes

import com.funlabyrinthe.core.*
import com.funlabyrinthe.core.pickling.*
import com.funlabyrinthe.core.scene.*

import com.funlabyrinthe.mazes.std.*

into final case class Square(
    field: Field,
    effect: Effect,
    tool: Tool,
    obstacle: Obstacle
):
  def present(context: PresentSquareContext): Batch[SceneNode] = {
    val presentField = field.present(context)
    val presentObstacle = obstacle.present(context)

    if obstacle.hideEffectAndTool then
      presentField ++ presentObstacle
    else
      presentField ++ effect.present(context) ++ tool.present(context) ++ presentObstacle
  }

  final def presentCeiling(context: PresentSquareContext): Batch[SceneNode] =
    field.presentCeiling(context)

  final def parts: List[SquareComponent] = List(field, effect, tool, obstacle)

  final def +(field: Field): Square =
    new Square(field, effect, tool, obstacle)
  final def +(effect: Effect): Square =
    new Square(field, effect, tool, obstacle)
  final def +(tool: Tool): Square =
    new Square(field, effect, tool, obstacle)
  final def +(obstacle: Obstacle): Square =
    new Square(field, effect, tool, obstacle)

  override def toString(): String = {
    given Universe = field.universe
    (field.toString +
        (if (effect != noEffect) ", " + effect.toString else "") +
        (if (tool != noTool) ", " + tool.toString else "") +
        (if (obstacle != noObstacle) ", " + obstacle.toString else ""))
  }

  private def hookEvent[T <: AbstractMoveContext](
    context: T,
    hook: (PosComponent, T) => Unit
  ): Boolean = {
    var xs = context.pos.posComponentsTopDown
    while !context.hooked && xs.nonEmpty do
      hook(xs.head, context)
      xs = xs.tail

    val hooked = context.hooked
    context.hooked = false
    hooked
  }

  def entering(context: EnteringContext): Unit = {
    if !hookEvent(context, _.entering(_)) then
      field.entering(context)
  }

  def exiting(context: ExitingContext): Unit = {
    if !hookEvent(context, _.exiting(_)) then
      field.exiting(context)
  }

  def entered(context: EnteredContext): Unit = {
    if !hookEvent(context, _.entered(_)) then
      field.entered(context)
      effect.entered(context)
  }

  def exited(context: ExitedContext): Unit = {
    if !hookEvent(context, _.exited(_)) then
      field.exited(context)
      effect.exited(context)
  }

  def execute(context: ExecuteContext): Unit = {
    if !hookEvent(context, _.execute(_)) then
      tool.find(context)
      effect.execute(context)
  }

  def pushing(context: EnteringContext): Unit = {
    if !hookEvent(context, _.pushing(_)) then
      obstacle.pushing(context)
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

  given FieldToSquare: Conversion[Field, Square] with
    def apply(field: Field): Square = field.toSquare

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
