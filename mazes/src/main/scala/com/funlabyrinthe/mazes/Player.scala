package com.funlabyrinthe
package mazes

import cps.customValueDiscard

import core._
import input.KeyEvent
import com.funlabyrinthe.core.graphics.*

import scala.annotation.unchecked.uncheckedVariance
import scala.collection.immutable.TreeSet
import scala.collection.mutable.{ Map => MutableMap }

final class Player(using ComponentInit)(@transient val corePlayer: CorePlayer)
    extends PosComponent with ReifiedPlayer
    derives Reflector {
  import universe._
  import Player._

  zIndex = DefaultZIndex

  painter += "Pawns/Player"

  var direction: Option[Direction] = None
  var hideCounter: Int = 0
  var color: Color = Color.Blue

  @transient
  private var coloredPainterCache: Option[(Painter, Color, Canvas)] = None

  override def reflect() = autoReflect[Player]

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

  override protected def doDraw(context: DrawContext): Unit = {
    import context._

    if isVisible then
      val plugins = this.plugins.toList
      for case plugin: PlayerPlugin <- plugins do
        plugin.drawBefore(this, context)

      val image = getColoredPainterImage()
      context.gc.drawImage(image, context.minX, context.minY)

      for case plugin: PlayerPlugin <- plugins.reverse do
        plugin.drawAfter(this, context)
    end if
  }

  private def getColoredPainterImage(): Image =
    coloredPainterCache match
      case Some((srcPainter, srcColor, cached)) if srcPainter == painter && srcColor == color =>
        cached

      case _ =>
        val cacheValid = painter.isComplete
        val computed = makeColoredPainter()
        if cacheValid then
          coloredPainterCache = Some((painter, color, computed))
        computed
  end getColoredPainterImage

  private def makeColoredPainter(): Canvas =
    val width = 30
    val height = 30
    val canvas = universe.graphicsSystem.createCanvas(width, height)
    val gc = canvas.getGraphicsContext2D()
    painter.drawTo(new DrawContext(gc, Rectangle2D(0, 0, width, height)))
    gc.multiplyByColor(0, 0, width, height, color)
    canvas
  end makeColoredPainter

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
          moveTo(context, execute = true)
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

  def moveTo(context: MoveContext, execute: Boolean): Control[Unit] = control {
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
    moveTo(dest, execute = false)
  }
}

object Player {
  type Perform = CorePlayer.Perform

  val DefaultZIndex = 1024
}
