package com.funlabyrinthe
package mazes

import cps.customValueDiscard

import core._
import graphics._
import input._

class PlayerController(val player: Player) extends Controller {
  import player.universe._

  private given Universe = player.universe

  override def viewSize: (Double, Double) = {
    player.position match {
      case Some(pos) =>
        val map = pos.map
        import map._
        ((ZoneWidth+2)*SquareWidth, (ZoneHeight+2)*SquareHeight)

      case None =>
        Controller.Dummy.viewSize
    }
  }

  override def drawView(context: DrawContext): Unit = {
    import context.gc

    super.drawView(context)

    if (player.position.isEmpty)
      return

    val playerPos = player.position.get

    def math_%(x: Int, y: Int) = {
      val mod = x % y
      if (mod < 0) mod + y else mod
    }
    def findZoneStart(pos: Int, zoneSize: Int) =
      pos - math_%(pos, zoneSize)

    val map = playerPos.map
    import map.{ SquareWidth, SquareHeight, ZoneWidth, ZoneHeight }

    val minX = findZoneStart(playerPos.x, ZoneWidth) - 1
    val minY = findZoneStart(playerPos.y, ZoneHeight) - 1
    val minPos = Position(minX, minY, playerPos.z)
    val visibleSquares = minPos until_+ (ZoneWidth+2, ZoneHeight+2)
    val visibleRefs = SquareRef.Range(map, visibleSquares)

    def posToRect(pos: Position) = {
      new Rectangle2D(
          (pos.x-minX)*SquareWidth, (pos.y-minY)*SquareHeight,
          SquareWidth, SquareHeight)
    }

    for (pos <- visibleSquares) {
      val ref = SquareRef(map, pos)
      val ctx = new DrawSquareContext(gc, posToRect(pos), Some(ref))
      ref().drawTo(ctx)
    }

    for {
      p <- components[Player]
      ref <- p.position
      if visibleRefs contains ref
    } {
      val ctx = new DrawSquareContext(gc, posToRect(ref.pos), Some(ref))
      p.drawTo(ctx)
    }

    for
      posComponent <- Mazes.mazes.posComponentsBottomUp
      ref <- posComponent.position
      if visibleRefs.contains(ref)
    do
      val ctx = new DrawSquareContext(gc, posToRect(ref.pos), Some(ref))
      posComponent.drawTo(ctx)
    end for

    // Plugins

    for (plugin <- player.plugins)
      plugin.drawView(player, context)
  }

  override def onKeyEvent(keyEvent: KeyEvent): Control[Unit] = control {
    import KeyCode._

    player.plugins foreach { plugin =>
      plugin.onKeyEvent(player, keyEvent)
    }

    if (player.playState == Player.PlayState.Playing) {
      val direction = keyEvent.code match {
        case Up    => Some(Direction.North)
        case Right => Some(Direction.East)
        case Down  => Some(Direction.South)
        case Left  => Some(Direction.West)
        case _     => None
      }

      if (direction.isDefined)
        player.move(direction.get, Some(keyEvent))
    }
  }
}
