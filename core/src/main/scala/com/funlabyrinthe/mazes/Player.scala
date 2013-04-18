package com.funlabyrinthe
package mazes

import core._
import input.KeyEvent

class Player(implicit uni: Universe)
extends NamedComponent with VisualComponent {

  import Player._

  var playState: PlayState = PlayState.Playing
  var position: Option[SquareRef[Map]] = None
  var direction: Option[Direction] = None

  val controller = new PlayerController(this)

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

object Player {
  final case class MoveTrampoline(delay: Int)

  sealed abstract class PlayState
  object PlayState {
    case object Playing extends PlayState
    case object Won extends PlayState
    case object Lost extends PlayState
  }
}
