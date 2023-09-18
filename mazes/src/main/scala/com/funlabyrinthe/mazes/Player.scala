package com.funlabyrinthe
package mazes

import cps.customValueDiscard

import core._
import input.KeyEvent

import scala.annotation.unchecked.uncheckedVariance
import scala.collection.immutable.TreeSet
import scala.collection.mutable.{ Map => MutableMap }

final class Player(using ComponentInit)(val corePlayer: CorePlayer) extends VisualComponent with ReifiedPlayer {
  import universe._
  import Player._

  var position: Option[SquareRef[Map]] = None
  var direction: Option[Direction] = None
  var hideCounter: Int = 0

  def mazesPlugins: List[PlayerPlugin] =
    plugins.toList.collect {
      case plugin: PlayerPlugin => plugin
    }
  end mazesPlugins

  override protected def autoProvideController(): Option[Controller] =
    if position.isEmpty then None
    else Some(new PlayerController(this))

  final def isVisible: Boolean = hideCounter <= 0

  final def hide(): Unit = hideCounter += 1

  final def show(): Unit = hideCounter -= 1

  override def drawTo(context: DrawContext) = {
    import context._

    if isVisible then
      val plugins = this.plugins.toList
      for case plugin: PlayerPlugin <- plugins do
        plugin.drawBefore(this, context)

      gc.fill = graphics.Color.Blue
      //gc.fillOval(rect.minX+3, rect.minY+3, rect.width-6, rect.height-6)
      gc.fillRect(rect.minX+8, rect.minY+8, rect.width-16, rect.height-16)

      for case plugin: PlayerPlugin <- plugins.reverse do
        plugin.drawAfter(this, context)
    end if
  }

  def move(dir: Direction,
      keyEvent: Option[KeyEvent]): Control[Unit] = control {

    require(position.isDefined,
        "move() requires an existing positon beforehand")

    if (playState == CorePlayer.PlayState.Playing) {
      val dest = position.get +> dir
      val context = new MoveContext(this, Some(dest), keyEvent)

      direction = Some(dir)
      if (exec(testMoveAllowed(context))) {
        if (position == context.src)
          moveTo(context)
      }
    }
  }

  def testMoveAllowed(context: MoveContext): Control[Boolean] = control {
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

  def moveTo(context: MoveContext, execute: Boolean = true): Control[Unit] = control {
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

  def moveTo(dest: SquareRef[Map], execute: Boolean): Control[Unit] = control {
    val context = new MoveContext(this, Some(dest), None)
    moveTo(context, execute = execute)
  }

  def moveTo(dest: SquareRef[Map]): Control[Unit] = control {
    moveTo(dest, execute = true)
  }
}

object Player {
  type Perform = CorePlayer.Perform
}
