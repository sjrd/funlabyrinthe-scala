package com.funlabyrinthe.mazes.std

import scala.collection.mutable

import com.funlabyrinthe.core.*
import com.funlabyrinthe.core.scene.*
import com.funlabyrinthe.mazes.*

class Lift(using ComponentInit) extends Effect:
  painter += "Stairs/Lift"
  var openedPainter: Painter = universe.EmptyPainter + "Stairs/OpenedLift"

  private val inUse = mutable.HashSet.empty[Player]

  override protected final def doPresent(context: PresentSquareContext): Batch[SceneNode] = {
    val showOpened = context.where.exists { ref =>
      universe.components[Player].exists { player =>
        player.position.contains(ref) && !inUse(player)
      }
    }
    if showOpened then
      presentOpened(context)
    else
      presentClosed(context)
  }

  protected def presentClosed(context: PresentSquareContext): Batch[SceneNode] =
    context.presentTiled(painter)

  protected def presentOpened(context: PresentSquareContext): Batch[SceneNode] =
    context.presentTiled(openedPainter)

  override def execute(context: ExecuteContext): Unit = {
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
    val targetFloor = player.showSelectNumberMessage(prompt, minFloor, maxFloor, default = thisFloor)

    // Move the player
    player.moveTo(pos.withZ(targetFloor), execute = false)

    // After a pause, open the lift and show the player
    temporize()
    inUse(player) = false
    player.show()
  }
end Lift
