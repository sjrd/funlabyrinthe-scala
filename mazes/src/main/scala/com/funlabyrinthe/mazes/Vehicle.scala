package com.funlabyrinthe.mazes

import com.funlabyrinthe.core.*
import com.funlabyrinthe.core.scene.*

abstract class Vehicle(using ComponentInit) extends PosComponent:
  private val plugin = subComponent(new VehiclePlugin(this))

  var northPainter: Painter = universe.EmptyPainter
  var eastPainter: Painter = universe.EmptyPainter
  var southPainter: Painter = universe.EmptyPainter
  var westPainter: Painter = universe.EmptyPainter

  private var controller: Option[Player] = None

  protected def dirPainter(direction: Direction): Painter = direction match
    case Direction.North => northPainter
    case Direction.East  => eastPainter
    case Direction.South => southPainter
    case Direction.West  => westPainter
  end dirPainter

  protected def attachController(player: Player): Unit =
    position = None
    controller = Some(player)
    player.plugins += plugin
  end attachController

  protected final def detachController(pos: SquareRef): Unit =
    detachController(Some(pos))

  protected def detachController(pos: Option[SquareRef]): Unit =
    for player <- controller do
      player.plugins -= plugin
      controller = None
      position = pos
  end detachController

  protected def detachController(): Unit =
    for player <- controller do
      detachController(player.position)
  end detachController

  def presentUnder(player: Player, context: PresentSquareContext): Batch[SceneNode] =
    context.presentTiled(dirPainter(player.direction))

  def presentAbove(player: Player, context: PresentSquareContext): Batch[SceneNode] =
    Batch.empty

  def controllerEntering(context: EnteringContext): Unit = ()

  def controllerExiting(context: ExitingContext): Unit = ()

  def controllerEntered(context: EnteredContext): Unit = ()

  def controllerExited(context: ExitedContext): Unit = ()

  def controllerPerform(player: CorePlayer): CorePlayer.Perform = PartialFunction.empty
end Vehicle
