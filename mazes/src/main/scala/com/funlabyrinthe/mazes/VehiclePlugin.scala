package com.funlabyrinthe.mazes

import com.funlabyrinthe.core.*
import com.funlabyrinthe.core.graphics.DrawContext

final class VehiclePlugin private[mazes] (using ComponentInit)(private val vehicle: Vehicle) extends PlayerPlugin:
  import universe.*

  override def drawBefore(player: Player, context: DrawContext): Unit =
    vehicle.drawBefore(player, context)

  override def drawAfter(player: Player, context: DrawContext): Unit =
    vehicle.drawAfter(player, context)

  override def moving(context: MoveContext): Control[Unit] =
    vehicle.controllerMoving(context)

  override def moved(context: MoveContext): Control[Unit] =
    vehicle.controllerMoved(context)

  override def perform(player: Player): Player.Perform =
    vehicle.controllerPerform(player)
end VehiclePlugin
