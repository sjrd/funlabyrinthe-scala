package com.funlabyrinthe
package mazes

import core._
import graphics._
import input._

class PlayerController(val player: Player) extends Controller {
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

    def posToRect(pos: Position) = {
      new Rectangle2D(
          (pos.x-minX)*SquareWidth, (pos.y-minY)*SquareHeight,
          SquareWidth, SquareHeight)
    }

    for (pos <- minPos until_+ (ZoneWidth+2, ZoneHeight+2)) {
      val ref = SquareRef(map, pos)
      val ctx = new DrawSquareContext(gc, posToRect(pos), Some(ref))
      ref().drawTo(ctx)
    }

    val playerRect = posToRect(playerPos)
    gc.fill = graphics.Color.BLUE
    gc.fillOval(playerRect.minX+3, playerRect.minY+3,
        playerRect.width-6, playerRect.height-6)
  }

  override def onKeyEvent(keyEvent: KeyEvent) {
    import javafx.scene.input.KeyCode._

    if (keyEvent.code.isArrowKey && player.position.isDefined) {
      val direction = (keyEvent.code.delegate: @unchecked) match {
        case UP | KP_UP => North
        case RIGHT | KP_RIGHT => East
        case DOWN | KP_DOWN => South
        case LEFT | KP_LEFT => West
      }
      player.applyMoveTrampoline(
          player.move(direction, Some(keyEvent)))
    }
  }
}
