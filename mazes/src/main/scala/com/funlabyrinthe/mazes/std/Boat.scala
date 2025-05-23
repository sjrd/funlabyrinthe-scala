package com.funlabyrinthe.mazes.std

import com.funlabyrinthe.core.*
import com.funlabyrinthe.mazes.*
import com.funlabyrinthe.mazes.Player.Perform

class Boat(using ComponentInit) extends Vehicle:
  category = ComponentCategory("boats", "Boats")

  painter += "Vehicles/Boat"
  northPainter += "Vehicles/BoatNorth"
  eastPainter += "Vehicles/BoatEast"
  southPainter += "Vehicles/BoatSouth"
  westPainter += "Vehicles/BoatWest"

  override protected def hookEntering(context: MoveContext): Unit = ()

  override protected def hookEntered(context: MoveContext): Unit = {
    attachController(context.player)

    // Hack to have the buoy disappear
    // TODO This should be implemented in a better, more generic way.
    context.player.plugins -= buoyPlugin
  }

  override def controllerMoving(context: MoveContext): Unit = {
    if context.player.direction != context.oldDirection then
      context.cancel()
  }

  override def controllerMoved(context: MoveContext): Unit = {
    if !context.dest.exists(_().field.isInstanceOf[Water]) then
      detachController(context.src)
  }

  override def controllerPerform(player: CorePlayer): Perform = {
    case GoOnWater => ()
  }

  override def dispatch[A]: PartialFunction[SquareMessage[A], A] = {
    case PlankInteraction(kind, player, passOverPos, leaveFrom, arriveAt) =>
      kind match
        case PlankInteraction.Kind.PassOver =>
          false
        case PlankInteraction.Kind.LeaveFrom =>
          arriveAt().field.isInstanceOf[Ground]
            && leaveFrom().obstacle == noObstacle
            && arriveAt().obstacle == noObstacle
        case PlankInteraction.Kind.ArriveAt =>
          leaveFrom().field.isInstanceOf[Ground]
            && leaveFrom().obstacle == noObstacle
            && arriveAt().obstacle == noObstacle
  }
end Boat
