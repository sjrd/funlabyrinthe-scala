package com.funlabyrinthe.mazes

import com.funlabyrinthe.core.*
import com.funlabyrinthe.core.input.KeyEvent
import com.funlabyrinthe.core.scene.*

import scala.annotation.unchecked.uncheckedVariance
import scala.collection.immutable.TreeSet
import scala.collection.mutable.{ Map => MutableMap }

final class Player(using ComponentInit)(@transient val corePlayer: CorePlayer)
    extends PosComponent with ReifiedPlayer {
  import universe._
  import Player._

  zIndex = DefaultZIndex

  painter += "Pawns/Player"

  var direction: Option[Direction] = None
  var hideCounter: Int = 0
  var color: RGBA = RGBA.Blue

  @transient @noinspect // TODO Can we make it so that we don't need this?
  def mazesPlugins: List[PlayerPlugin] =
    plugins.toList.collect {
      case plugin: PlayerPlugin => plugin
    }
  end mazesPlugins

  override protected def autoProvideController(): Option[Controller] =
    if position.isEmpty then None
    else Some(new PlayerController(this))

  @transient @noinspect // TODO Can we make it so that we don't need this?
  final def isVisible: Boolean = hideCounter <= 0

  final def hide(): Unit = hideCounter += 1

  final def show(): Unit = hideCounter -= 1

  override protected def doPresent(context: PresentSquareContext): Batch[SceneNode] = {
    import context.*

    if !isVisible then {
      Batch.empty
    } else {
      var result = context.presentTiled(painter).map {
        case g: Graphic => g.copy(material = g.material.copy(tint = color.withAlpha(1.0), alpha = color.alpha))
        case other      => other
      }

      for case plugin: PlayerPlugin <- plugins.toList.reverse do
        result = plugin.presentUnder(this, context) ++ result ++ plugin.presentAbove(this, context)

      result
    }
  }

  def move(dir: Direction, keyEvent: Option[KeyEvent]): Unit = {
    require(position.isDefined,
        "move() requires an existing positon beforehand")

    if (playState == CorePlayer.PlayState.Playing) {
      val dest = position.get +> dir
      val context = new MoveContext(this, Some(dest), keyEvent)

      direction = Some(dir)
      if (testMoveAllowed(context)) {
        if (position == context.src)
          moveTo(context, execute = true)
      }
    }
  }

  def testMoveAllowed(context: MoveContext): Boolean = {
    import context._

    // Can't use `return` within a CPS method, so this is a bit nested

    setPosToSource()

    pos().exiting(context)
    if (cancelled)
      false
    else {
      mazesPlugins.foreach { plugin =>
        if (!cancelled)
          plugin.moving(context)
      }

      if (cancelled)
        false
      else {
        setPosToDest()

        pos().entering(context)
        if (cancelled)
          false
        else {
          pos().pushing(context)
          if (cancelled)
            false
          else
            true
        }
      }
    }
  }

  def moveTo(context: MoveContext, execute: Boolean): Unit = {
    import context._

    position = dest

    setPosToSource()
    pos().exited(context)

    setPosToDest()

    mazesPlugins.foreach(_.moved(context))

    pos().entered(context)

    if (execute && position == dest) {
      pos().execute(context)

      if (context.goOnMoving && player.direction.isDefined) {
        sleep(context.temporization)
        move(player.direction.get, None)
      }
    }
  }

  def moveTo(dest: SquareRef, execute: Boolean): Unit = {
    val context = new MoveContext(this, Some(dest), None)
    moveTo(context, execute = execute)
  }

  def moveTo(dest: SquareRef): Unit = {
    moveTo(dest, execute = false)
  }
}

object Player {
  type Perform = CorePlayer.Perform

  val DefaultZIndex = 1024
}
