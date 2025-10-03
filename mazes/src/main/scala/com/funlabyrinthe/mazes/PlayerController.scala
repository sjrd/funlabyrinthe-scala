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

    val ViewBorderSize = 1 // TODO This should be configurable

    def findZoneStart(pos: Int, zoneSize: Int, mapSize: Int): Int =
      if player.isPlaying || (pos >= 0 && pos < mapSize) || pos < -ViewBorderSize || pos >= mapSize + ViewBorderSize then
        pos - Math.floorMod(pos, zoneSize)
      else
        // When we're done, if we're barely out of the map (within the ViewBorderSize), force the view inside the map
        if pos < 0 then 0
        else mapSize - zoneSize

    val map = playerPos.map
    import map.{ SquareWidth, SquareHeight, zoneWidth, zoneHeight }

    val minX = findZoneStart(playerPos.x, zoneWidth, map.dimensions.x) - ViewBorderSize
    val minY = findZoneStart(playerPos.y, zoneHeight, map.dimensions.y) - ViewBorderSize
    val minPos = Position(minX, minY, playerPos.z)
    val visibleSquares = minPos until_+ (zoneWidth + 2*ViewBorderSize, zoneHeight + 2*ViewBorderSize)
    val visibleRefs = SquareRef.Range(map, visibleSquares)

    def posToRect(pos: Position) = {
      new Rectangle2D(
          (pos.x-minX)*SquareWidth, (pos.y-minY)*SquareHeight,
          SquareWidth, SquareHeight)
    }

    // Squares

    for (pos <- visibleSquares) {
      val ref = SquareRef(map, pos)
      val ctx = new DrawSquareContext(gc, context.tickCount, posToRect(pos), Some(ref), drawPurpose)
      ref().drawTo(ctx)
    }

    // PosComponents

    for
      posComponent <- posComponentsBottomUp
      ref <- posComponent.position
      if visibleRefs.contains(ref)
    do
      val ctx = new DrawSquareContext(gc, context.tickCount, posToRect(ref.pos), Some(ref), drawPurpose)
      posComponent.drawTo(ctx)
    end for

    // Square ceilings

    for pos <- visibleSquares do
      val ref = SquareRef(map, pos)
      val ctx = new DrawSquareContext(gc, context.tickCount, posToRect(pos), Some(ref), drawPurpose)
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
