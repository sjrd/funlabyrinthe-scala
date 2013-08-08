package com.funlabyrinthe
package mazes

import core._
import graphics._
import input._

class PlayerController(val player: Player) extends Controller {
  import player.universe._

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

  override def drawView(context: DrawContext) {
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

    // Plugins

    for (plugin <- player.plugins)
      plugin.drawView(context)
  }

  override def onKeyEvent(keyEvent: KeyEvent): Unit @control = {
    import javafx.scene.input.KeyCode._

    player.plugins cforeach { plugin =>
      plugin.onKeyEvent(keyEvent)
    }

    if (player.playState == Player.PlayState.Playing) {
      if (keyEvent.code.isArrowKey && player.position.isDefined) {
        val direction = (keyEvent.code.delegate: @unchecked) match {
          case UP | KP_UP => North
          case RIGHT | KP_RIGHT => East
          case DOWN | KP_DOWN => South
          case LEFT | KP_LEFT => West
        }
        player.move(direction, Some(keyEvent))
      }
    }
  }
}
