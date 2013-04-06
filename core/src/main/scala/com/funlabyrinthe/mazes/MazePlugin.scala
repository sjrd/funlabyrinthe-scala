package com.funlabyrinthe.mazes

import com.funlabyrinthe.core._

import scalafx.scene.input.KeyEvent

final class MazePlugin[U <: Universe](val universe: U)
extends UniversePlugin
   with Maps[U] {

  import universe._

  type StaticPlayerPlugin = MazePlayer

  override protected def createStaticPlayerPlugin(player: Player) =
    new MazePlayer(player)

  type PlayState = MazePlugin.PlayState
  val PlayState = MazePlugin.PlayState

  type MoveTrampoline = MazePlugin.MoveTrampoline
  val MoveTrampoline = MazePlugin.MoveTrampoline

  class MazePlayer(val player: Player) extends universe.StaticPlayerPlugin {
    var playState: PlayState = PlayState.Playing
    var position: Option[SquareRef] = None
    var direction: Option[Direction] = None

    val mazeController = new MazeController(this)

    def move(dir: Direction,
        keyEvent: Option[KeyEvent]): Option[MoveTrampoline] = {

      require(position.isDefined,
          "move() requires an existing positon beforehand")

      if (playState != PlayState.Playing) None
      else {
        val dest = position.get +> dir
        val context = new MoveContext(this, Some(dest), keyEvent)

        direction = Some(dir)
        if (!testMoveAllowed(context)) None
        else {
          if (position == context.src)
            moveTo(context)

          if (context.goOnMoving) Some(MoveTrampoline(context.temporization))
          else None
        }
      }
    }

    def testMoveAllowed(context: MoveContext): Boolean = {
      import context._

      setPosToSource()

      pos().exiting(context)
      if (cancelled)
        return false
      // TODO Moving in plugins

      setPosToDest()
      pos().entering(context)
      if (cancelled)
        return false
      pos().pushing(context)
      if (cancelled)
        return false

      return true
    }

    def moveTo(context: MoveContext, execute: Boolean = true) {
      import context._

      position = dest

      setPosToSource()
      pos().exited(context)

      setPosToDest()
      // TODO Moved in plugins
      pos().entered(context)

      if (execute && position == dest)
        pos().execute(context)
    }

    @scala.annotation.tailrec
    final def applyMoveTrampoline(trampoline: Option[MoveTrampoline]) {
      if (trampoline.isDefined) {
        Thread.sleep(trampoline.get.delay)
        if (direction.isDefined)
          applyMoveTrampoline(move(direction.get, None))
      }
    }
  }

  class MoveContext(val player: MazePlayer, val dest: Option[SquareRef],
      val keyEvent: Option[KeyEvent] = None) {

    private var _pos: SquareRef = _

    val src = player.position
    def pos = _pos
    val oldDirection = player.direction

    def setPosToSource() = _pos = src.get
    def setPosToDest() = _pos = dest.get

    def isRegular =
      src.isDefined && dest.isDefined && (src.get.map eq dest.get.map)

    var cancelled = false
    var goOnMoving = false
    var hooked = false

    var temporization = 500
  }

  class MazeController(val player: MazePlayer) extends PlayerController {
    // TODO
    private def zoneWidth = 7
    private def zoneHeight = 7

    def viewSize = ((zoneWidth+2)*SquareSize, (zoneHeight+2)*SquareSize)

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
      val minX = findZoneStart(playerPos.x, zoneWidth) - 1
      val minY = findZoneStart(playerPos.y, zoneHeight) - 1
      val minPos = Position(minX, minY, playerPos.z)

      def posToRect(pos: Position) = {
        new Rectangle2D(
            (pos.x-minX)*SquareSize, (pos.y-minY)*SquareSize,
            SquareSize, SquareSize)
      }

      for (pos <- minPos until_+ (zoneWidth+2, zoneHeight+2)) {
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

      if (keyEvent.code.isArrowKey) {
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
}

object MazePlugin {
  final case class MoveTrampoline(delay: Int)

  sealed abstract class PlayState
  object PlayState {
    case object Playing extends PlayState
    case object Won extends PlayState
    case object Lost extends PlayState
  }
}
