package com.funlabyrinthe.mazes

import com.funlabyrinthe.core.*
import com.funlabyrinthe.core.input.KeyEvent
import com.funlabyrinthe.core.scene.*

import scala.annotation.unchecked.uncheckedVariance
import scala.collection.immutable.TreeSet
import scala.collection.mutable.{ Map => MutableMap }
import scala.util.boundary

final class Player(using ComponentInit)(@transient val corePlayer: CorePlayer)
    extends PosComponent with ReifiedPlayer {
  import universe._
  import Player._

  zIndex = DefaultZIndex

  painter += "Pawns/Player"

  var direction: Direction = Direction.South // typically: facing the camera
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
      val originalPos = position.get
      val previousDirection = direction
      direction = dir

      if testMoveAllowed(previousDirection, keyEvent) then
        moveTo(originalPos +> direction, execute = true)
    }
  }

  def testMoveAllowed(previousDirection: Direction, keyEvent: Option[KeyEvent]): Boolean =
    testMoveAllowed(position.get +> direction, previousDirection, keyEvent)

  def testMoveAllowed(dest: SquareRef, previousDirection: Direction,
      keyEvent: Option[KeyEvent]): Boolean = boundary {

    val src = position.get

    // Exiting

    val exitingTemporization = {
      val exitingContext = ExitingContext(this, previousDirection, dest, keyEvent)

      src().exiting(exitingContext)
      if exitingContext.canceled || !position.contains(src) then
        boundary.break(false)

      foreachPlugin { plugin =>
        plugin.exiting(exitingContext)
        if exitingContext.canceled || !position.contains(src) then
          boundary.break(false)
      }

      exitingContext.temporization
    }

    // Entering

    val enteringContext = EnteringContext(this, previousDirection, dest, keyEvent)
    enteringContext.temporization = exitingTemporization

    foreachPlugin { plugin =>
      plugin.entering(enteringContext)
      if enteringContext.canceled || !position.contains(src) then
        boundary.break(false)
    }

    dest().entering(enteringContext)
    if enteringContext.canceled || !position.contains(src) then
      boundary.break(false)

    // Pushing

    dest().pushing(enteringContext)
    if enteringContext.canceled || !position.contains(src) then
      boundary.break(false)

    true
  }

  def moveTo(dest: SquareRef): Unit = {
    moveTo(dest, execute = false)
  }

  def moveTo(dest: SquareRef, execute: Boolean): Unit =
    moveTo(Some(dest), execute)

  def moveTo(optDest: Option[SquareRef], execute: Boolean): Unit = boundary {
    val optSrc = position
    position = optDest

    // Exited

    val exitedTemporization = optSrc match {
      case Some(src) =>
        val exitedContext = ExitedContext(this, src)

        src().exited(exitedContext)
        if position != optDest then
          boundary.break(false)

        foreachPlugin { plugin =>
          plugin.exited(exitedContext)
          if position != optDest then
            boundary.break()
        }

        exitedContext.temporization

      case None =>
        500
    }

    optDest match {
      case Some(dest) =>
        // Entered

        val (enteredTemporization, enteredGoOnMoving) = {
          val enteredContext = EnteredContext(this, optSrc)
          enteredContext.temporization = exitedTemporization

          foreachPlugin { plugin =>
            plugin.entered(enteredContext)
            if !position.contains(dest) then
              boundary.break()
          }

          dest().entered(enteredContext)
          if !position.contains(dest) then
            boundary.break()

          (enteredContext.temporization, enteredContext.goOnMoving)
        }

        // Execute

        if execute then {
          val executeContext = ExecuteContext(this)
          executeContext.temporization = enteredTemporization
          executeContext.goOnMoving = enteredGoOnMoving

          dest().execute(executeContext)
          if !position.contains(dest) then
            boundary.break()

          if executeContext.goOnMoving then
            sleep(executeContext.temporization)
            move(direction, keyEvent = None)
        }

      case None =>
        ()
    }
  }

  private inline def foreachPlugin(inline f: PlayerPlugin => Unit): Unit = {
    // Use a while loop so that boundary.break's can become jumps
    var rest = mazesPlugins
    while rest.nonEmpty do
      f(rest.head)
      rest = rest.tail
  }
}

object Player {
  type Perform = CorePlayer.Perform

  val DefaultZIndex = 1024
}
