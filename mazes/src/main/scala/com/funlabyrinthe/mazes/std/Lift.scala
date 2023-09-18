package com.funlabyrinthe.mazes.std

import cps.customValueDiscard

import com.funlabyrinthe.core.*
import com.funlabyrinthe.mazes.*

class Lift(using ComponentInit) extends Effect:
  import universe.*

  painter += "Stairs/Lift"
  var openedPainter: Painter = EmptyPainter + "Stairs/OpenedLift"

  private val inUse = CorePlayer.mutable.SimplePerPlayerData[Boolean](false)

  override final def drawTo(context: DrawSquareContext[Map]): Unit =
    val showOpened = context.where.exists { ref =>
      universe.components[Player].exists { player =>
        player.position.contains(ref) && !inUse(player)
      }
    }
    if showOpened then
      drawOpenedTo(context)
    else
      drawClosedTo(context)
  end drawTo

  protected def drawClosedTo(context: DrawSquareContext[Map]): Unit =
    painter.drawTo(context)

  protected def drawOpenedTo(context: DrawSquareContext[Map]): Unit =
    openedPainter.drawTo(context)

  override def execute(context: MoveContext): Control[Unit] = control {
    import context.*

    // Show the lift as opened for a time, then close it
    temporize()
    player.hide()
    inUse(player) = true

    // Find the extent of the shaft
    def isLiftAt(z: Int): Boolean =
      pos.withZ(z)().effect.isInstanceOf[Lift]
    val thisFloor = pos.pos.z
    val floorCount = pos.map.dimensions.z
    val minFloor = (thisFloor to 0 by -1).find(!isLiftAt(_)).getOrElse(-1) + 1
    val maxFloor = (thisFloor until floorCount).find(!isLiftAt(_)).getOrElse(floorCount) - 1

    // Ask the player what floor they want to go to
    val prompt = "Which floor do you want to go to?"
    val targetFloor = exec(player.showSelectNumberMessage(prompt, minFloor, maxFloor, default = thisFloor))

    // Move the player
    player.moveTo(pos.withZ(targetFloor), execute = false)

    // After a pause, open the lift and show the player
    temporize()
    inUse(player) = false
    player.show()
  }
end Lift
