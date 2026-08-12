package com.funlabyrinthe.mazes

import com.funlabyrinthe.core.*
import com.funlabyrinthe.core.scene.*

final class VehiclePlugin private[mazes] (using ComponentInit)(private val vehicle: Vehicle) extends PlayerPlugin:
  import universe.*

  override def presentUnder(player: Player, context: PresentSquareContext): Batch[SceneNode] =
    vehicle.presentUnder(player, context)

  override def presentAbove(player: Player, context: PresentSquareContext): Batch[SceneNode] =
    vehicle.presentAbove(player, context)

  override def exiting(context: ExitingContext): Unit =
    vehicle.controllerExiting(context)

  override def entering(context: EnteringContext): Unit =
    vehicle.controllerEntering(context)

  override def exited(context: ExitedContext): Unit =
    vehicle.controllerExited(context)

  override def entered(context: EnteredContext): Unit =
    vehicle.controllerEntered(context)

  override def perform(player: CorePlayer): Player.Perform =
    vehicle.controllerPerform(player)
end VehiclePlugin
