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

  protected def dirPainter(direction: Option[Direction]): Painter = direction match
    case Some(Direction.North) => northPainter
    case Some(Direction.East)  => eastPainter
    case Some(Direction.South) => southPainter
    case Some(Direction.West)  => westPainter
    case None                  => painter
  end dirPainter

  protected def attachController(player: Player): Unit =
    position = None
    controller = Some(player)
    player.plugins += plugin
  end attachController

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

  def controllerMoving(context: MoveContext): Unit = ()

  def controllerMoved(context: MoveContext): Unit = ()

  def controllerPerform(player: CorePlayer): CorePlayer.Perform = PartialFunction.empty
end Vehicle
