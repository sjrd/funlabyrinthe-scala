package com.funlabyrinthe.mazes

import com.funlabyrinthe.core.*
import com.funlabyrinthe.core.input.*
import com.funlabyrinthe.core.scene.*

import com.funlabyrinthe.mazes.std.*

class PlayerController(val player: Player) extends Controller {
  import player.universe._

  private given Universe = player.universe

  private final val ViewBorderSize = 1 // TODO This should be configurable

  override def viewSize: Size = {
    player.position match {
      case Some(pos) =>
        val map = pos.map
        import map._
        Size((zoneWidth + 2) * SquareWidth, (zoneHeight + 2) * SquareHeight)

      case None =>
        Controller.Dummy.viewSize
    }
  }

  def present(): SceneUpdateFragment = {
    if (player.position.isEmpty)
      return SceneUpdateFragment.empty

    val tickCount = player.universe.tickCount
    val drawPurpose = DrawPurpose.PlayerView(player)

    val playerPos = player.position.get

    val map = playerPos.map
    import map.{ SquareWidth, SquareHeight, zoneWidth, zoneHeight }

    val halfSquareWidth = SquareWidth / 2
    val halfSquareHeight = SquareHeight / 2

    val minX = findZoneStart(playerPos.x, zoneWidth, map.dimensions.x) - ViewBorderSize
    val minY = findZoneStart(playerPos.y, zoneHeight, map.dimensions.y) - ViewBorderSize
    val minPos = Position(minX, minY, playerPos.z)
    val visibleSquares = minPos until_+ (zoneWidth + 2*ViewBorderSize, zoneHeight + 2*ViewBorderSize)
    val visibleRefs = SquareRef.Range(map, visibleSquares)

    val cellSize = Size(SquareWidth, SquareHeight)

    def posToCenter(ref: SquareRef): Point =
      Point((ref.pos.x-minX)*SquareWidth + halfSquareWidth, (ref.pos.y-minY)*SquareHeight + halfSquareHeight)

    // Squares

    val presentedSquares = Batch.from(
      for ref <- visibleRefs yield
        Group(ref().present(PresentSquareContext(tickCount, Some(ref), drawPurpose, cellSize))).moveBy(posToCenter(ref))
    )

    // PosComponents

    val presentedPosComponents = Batch.from(
      for
        posComponent <- posComponentsBottomUp
        ref <- posComponent.position
        if visibleRefs.contains(ref)
      yield
        Group(posComponent.present(PresentSquareContext(tickCount, Some(ref), drawPurpose, cellSize))).moveBy(posToCenter(ref))
    )

    // Square ceilings

    val presentCeilings = Batch.from(
      for ref <- visibleRefs yield
        Group(ref().presentCeiling(PresentSquareContext(tickCount, Some(ref), drawPurpose, cellSize))).moveBy(posToCenter(ref))
    )

    // Put it all together

    val allBatches =
      presentedSquares
        ++ presentedPosComponents
        ++ presentCeilings

    val baseFragment = SceneUpdateFragment(allBatches)

    // Plugins

    player.plugins.foldLeft(baseFragment) { (prev, plugin) =>
      prev ++ plugin.presentView(player.corePlayer, viewSize)
    }
  }

  private def findZoneStart(pos: Int, zoneSize: Int, mapSize: Int): Int = {
    if player.isPlaying || (pos >= 0 && pos < mapSize) || pos < -ViewBorderSize || pos >= mapSize + ViewBorderSize then
      pos - Math.floorMod(pos, zoneSize)
    else
      // When we're done, if we're barely out of the map (within the ViewBorderSize), force the view inside the map
      if pos < 0 then 0
      else mapSize - zoneSize
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
