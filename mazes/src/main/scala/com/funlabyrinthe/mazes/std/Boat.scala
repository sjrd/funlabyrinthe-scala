package com.funlabyrinthe.mazes.std

import cps.customValueDiscard

import com.funlabyrinthe.core.*
import com.funlabyrinthe.mazes.*
import com.funlabyrinthe.mazes.Player.Perform

class Boat(using ComponentInit) extends Vehicle:
  name = "Barque"
  category = ComponentCategory("boats", "Boats")

  painter += "Vehicles/Boat"
  northPainter += "Vehicles/BoatNorth"
  eastPainter += "Vehicles/BoatEast"
  southPainter += "Vehicles/BoatSouth"
  westPainter += "Vehicles/BoatWest"

  override protected def hookEntering(context: MoveContext): Control[Unit] =
    doNothing()

  override protected def hookEntered(context: MoveContext): Control[Unit] = control {
    attachController(context.player)

    // Hack to have the buoy disappear
    // TODO This should be implemented in a better, more generic way.
    context.player.plugins -= Mazes.mazes.BuoyPlugin
  }

  override def controllerMoving(context: MoveContext): Control[Unit] = control {
    if context.player.direction != context.oldDirection then
      context.cancel()
  }

  override def controllerMoved(context: MoveContext): Control[Unit] = control {
    if !context.dest.exists(_().field.isInstanceOf[Water]) then
      detachController(context.src)
  }

  override def controllerPerform(player: CorePlayer): Perform = {
    case GoOnWater =>
      doNothing()
  }

  override def dispatch[A]: PartialFunction[SquareMessage[A], A] = {
    case PlankInteraction(kind, player, passOverPos, leaveFrom, arriveAt) =>
      val NoObstacle = Mazes.mazes.NoObstacle
      kind match
        case PlankInteraction.Kind.PassOver =>
          false
        case PlankInteraction.Kind.LeaveFrom =>
          arriveAt().field.isInstanceOf[Ground]
            && leaveFrom().obstacle == NoObstacle
            && arriveAt().obstacle == NoObstacle
        case PlankInteraction.Kind.ArriveAt =>
          leaveFrom().field.isInstanceOf[Ground]
            && leaveFrom().obstacle == NoObstacle
            && arriveAt().obstacle == NoObstacle
  }
end Boat
