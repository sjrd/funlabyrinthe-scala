package com.funlabyrinthe.mazes

import com.funlabyrinthe.core.*

final class VehiclePlugin private[mazes] (using ComponentInit)(private val vehicle: Vehicle) extends PlayerPlugin:
  import universe.*

  override def drawBefore(player: Player, context: DrawSquareContext): Unit =
    vehicle.drawBefore(player, context)

  override def drawAfter(player: Player, context: DrawSquareContext): Unit =
    vehicle.drawAfter(player, context)

  override def moving(context: MoveContext): Unit =
    vehicle.controllerMoving(context)

  override def moved(context: MoveContext): Unit =
    vehicle.controllerMoved(context)

  override def perform(player: CorePlayer): Player.Perform =
    vehicle.controllerPerform(player)
end VehiclePlugin
