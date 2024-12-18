package com.funlabyrinthe.mazes

import com.funlabyrinthe.core.*
import com.funlabyrinthe.core.graphics.*
import com.funlabyrinthe.core.input.*

import com.funlabyrinthe.mazes.std.*

class PlayerController(val player: Player) extends Controller {
  import player.universe._

  private given Universe = player.universe

  override def viewSize: (Double, Double) = {
    player.position match {
      case Some(pos) =>
        val map = pos.map
        import map._
        ((zoneWidth + 2) * SquareWidth, (zoneHeight + 2) * SquareHeight)

      case None =>
        Controller.Dummy.viewSize
    }
  }

  override def drawView(context: DrawContext): Unit = {
    import context.gc

    super.drawView(context)

    if (player.position.isEmpty)
      return

    val drawPurpose = DrawPurpose.PlayerView(player)

    val playerPos = player.position.get

    def math_%(x: Int, y: Int) = {
      val mod = x % y
      if (mod < 0) mod + y else mod
    }
    def findZoneStart(pos: Int, zoneSize: Int) =
      pos - math_%(pos, zoneSize)

    val map = playerPos.map
    import map.{ SquareWidth, SquareHeight, zoneWidth, zoneHeight }

    val minX = findZoneStart(playerPos.x, zoneWidth) - 1
    val minY = findZoneStart(playerPos.y, zoneHeight) - 1
    val minPos = Position(minX, minY, playerPos.z)
    val visibleSquares = minPos until_+ (zoneWidth + 2, zoneHeight + 2)
    val visibleRefs = SquareRef.Range(map, visibleSquares)

    def posToRect(pos: Position) = {
      new Rectangle2D(
          (pos.x-minX)*SquareWidth, (pos.y-minY)*SquareHeight,
          SquareWidth, SquareHeight)
    }

    // Squares

    for (pos <- visibleSquares) {
      val ref = SquareRef(map, pos)
      val ctx = new DrawSquareContext(gc, posToRect(pos), Some(ref), drawPurpose)
      ref().drawTo(ctx)
    }

    // PosComponents

    for
      posComponent <- posComponentsBottomUp
      ref <- posComponent.position
      if visibleRefs.contains(ref)
    do
      val ctx = new DrawSquareContext(gc, posToRect(ref.pos), Some(ref), drawPurpose)
      posComponent.drawTo(ctx)
    end for

    // Square ceilings

    for pos <- visibleSquares do
      val ref = SquareRef(map, pos)
      val ctx = new DrawSquareContext(gc, posToRect(pos), Some(ref), drawPurpose)
      ref().drawCeilingTo(ctx)
    end for

    // Plugins

    for (plugin <- player.plugins)
      plugin.drawView(player.corePlayer, context)
  }

  override def onKeyEvent(keyEvent: KeyEvent): Unit = {
    val iter = player.plugins.iterator
    while iter.hasNext do
      iter.next().onKeyEvent(player.corePlayer, keyEvent)

    if (player.playState == CorePlayer.PlayState.Playing) {
      val direction = keyEvent.keyString match {
        case KeyStrings.ArrowUp    => Some(Direction.North)
        case KeyStrings.ArrowRight => Some(Direction.East)
        case KeyStrings.ArrowDown  => Some(Direction.South)
        case KeyStrings.ArrowLeft  => Some(Direction.West)
        case _                     => None
      }

      if (direction.isDefined)
        player.move(direction.get, Some(keyEvent))
    }
  }
}
