package com.funlabyrinthe
package mazes

import core._
import input.KeyEvent

import scala.collection.immutable.TreeSet
import scala.collection.mutable

class Player(override implicit val universe: MazeUniverse)
extends NamedComponent with VisualComponent {

  import universe._
  import Player._

  type Perform = PartialFunction[Any, Unit]

  var playState: PlayState = PlayState.Playing
  var position: Option[SquareRef[Map]] = None
  var direction: Option[Direction] = None

  var plugins: TreeSet[PlayerPlugin] = TreeSet.empty

  val controller = new PlayerController(this)

  override def drawTo(context: DrawContext) = {
    import context._

    val plugins = this.plugins.toList
    for (plugin <- plugins)
      plugin.drawBefore(context)

    gc.fill = graphics.Color.BLUE
    gc.fillOval(rect.minX+3, rect.minY+3, rect.width-6, rect.height-6)

    for (plugin <- plugins.reverse)
      plugin.drawAfter(context)
  }

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

    for (plugin <- plugins) {
      plugin.moving(context)
      if (cancelled)
        return false
    }

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

    for (plugin <- plugins) {
      plugin.moved(context)
    }

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

  def isAbleTo(action: Any): Boolean = {
    (plugins.exists(p => p.perform(this).isDefinedAt(action)) ||
        ItemDef.all.exists(i => i.perform(this).isDefinedAt(action)))
  }

  def perform(action: Any): Unit = {
    if (!tryPerform(action))
      assert(false, "must not call perform(action) if !isAbleTo(action)")
  }

  def tryPerform(action: Any): Boolean = {
    for (plugin <- plugins) {
      val perform = plugin.perform(this)
      if (perform.isDefinedAt(action)) {
        perform(action)
        return true
      }
    }

    for (item <- ItemDef.all) {
      val perform = item.perform(this)
      if (perform.isDefinedAt(action)) {
        perform(action)
        return true
      }
    }

    return false
  }

  // DSL
  def can(action: Any): Boolean = tryPerform(action)
  def cannot(action: Any): Boolean = !tryPerform(action)

  def has(item: ItemDef): Boolean = item.count(this) > 0
  def has(count: Int, item: ItemDef): Boolean = item.count(this) >= count
}

object Player {
  final case class MoveTrampoline(delay: Int)

  sealed abstract class PlayState
  object PlayState {
    case object Playing extends PlayState
    case object Won extends PlayState
    case object Lost extends PlayState
  }

  trait PerPlayerData[A] {
    private val data = new mutable.WeakHashMap[Player, A]

    def apply(player: Player): A = data.getOrElseUpdate(player, initial(player))
    def update(player: Player, value: A): Unit = data.put(player, value)

    protected def initial(player: Player): A
  }

  class SimplePerPlayerData[A](default: A) extends PerPlayerData[A] {
    protected def initial(player: Player) = default
  }
}
