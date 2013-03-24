package com.funlabyrinthe.mazes

import com.funlabyrinthe.core._

import scalafx.scene.input.KeyEvent

final class MazePlugin(val universe: Universe) extends UniversePlugin
                                                  with Maps {
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
