package com.funlabyrinthe.mazes

import com.funlabyrinthe.core.*
import com.funlabyrinthe.core.scene.*

final class VehiclePlugin private[mazes] (using ComponentInit)(private val vehicle: Vehicle) extends PlayerPlugin:
  import universe.*

  override def presentUnder(player: Player, context: PresentSquareContext): Batch[SceneNode] =
    vehicle.presentUnder(player, context)

  override def presentAbove(player: Player, context: PresentSquareContext): Batch[SceneNode] =
    vehicle.presentAbove(player, context)

  override def moving(context: MoveContext): Unit =
    vehicle.controllerMoving(context)

  override def moved(context: MoveContext): Unit =
    vehicle.controllerMoved(context)

  override def perform(player: CorePlayer): Player.Perform =
    vehicle.controllerPerform(player)
end VehiclePlugin
