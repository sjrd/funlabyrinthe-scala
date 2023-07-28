package com.funlabyrinthe
package mazes

import cps.customValueDiscard

import core._
import input.KeyEvent

import scala.annotation.unchecked.uncheckedVariance
import scala.collection.immutable.TreeSet
import scala.collection.mutable.{ Map => MutableMap }

final class Player(using ComponentInit) extends VisualComponent {
  import universe._
  import Player._

  var playState: PlayState = PlayState.Playing
  var position: Option[SquareRef[Map]] = None
  var direction: Option[Direction] = None

  var plugins: TreeSet[PlayerPlugin] = TreeSet.empty

  val controller = new PlayerController(this)

  override def drawTo(context: DrawContext) = {
    import context._

    val plugins = this.plugins.toList
    for (plugin <- plugins)
      plugin.drawBefore(this, context)

    gc.fill = graphics.Color.Blue
    //gc.fillOval(rect.minX+3, rect.minY+3, rect.width-6, rect.height-6)
    gc.fillRect(rect.minX+8, rect.minY+8, rect.width-16, rect.height-16)

    for (plugin <- plugins.reverse)
      plugin.drawAfter(this, context)
  }

  def move(dir: Direction,
      keyEvent: Option[KeyEvent]): Control[Unit] = control {

    require(position.isDefined,
        "move() requires an existing positon beforehand")

    if (playState == PlayState.Playing) {
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
      plugins foreach { plugin =>
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

    plugins.foreach(_.moved(context))

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

  def isAbleTo(action: Any): Boolean = {
    (plugins.exists(p => p.perform(this).isDefinedAt(action)) ||
        ItemDef.all.exists(i => i.perform(this).isDefinedAt(action)))
  }

  def perform(action: Any): Control[Unit] = control {
    if (!exec(tryPerform(action)))
      assert(false, "must not call perform(action) if !isAbleTo(action)")
  }

  def tryPerform(action: Any): Control[Boolean] = control {
    val perform = {
      plugins.collectFirst({
        case p if p.perform(this).isDefinedAt(action) => p.perform(this)
      }) orElse ItemDef.all.collectFirst({
        case i if i.perform(this).isDefinedAt(action) => i.perform(this)
      })
    }

    if (perform.isDefined) {
      perform.get(action)
      true
    } else {
      false
    }
  }

  def dispatch(message: Any): Control[Unit] = control {
    var handled = false
    val iter = plugins.iterator
    while (!handled && iter.hasNext) {
      val plugin = iter.next()
      if (exec(plugin.onMessage(this, message)))
        handled = true
    }
  }

  def win(): Unit = {
    playState = PlayState.Won

    for (player <- components[Player]; if player ne this)
      player.playState = PlayState.Lost

    universe.terminate()
  }

  def lose(): Unit = {
    playState = PlayState.Lost

    if (components[Player].forall(_.playState != PlayState.Playing))
      universe.terminate()
  }

  // DSL
  def can(action: Any): Control[Boolean] = tryPerform(action)
  def cannot(action: Any): Control[Boolean] = tryPerform(action).map(!_)

  def has(item: ItemDef): Boolean = item.count(this) > 0
  def has(count: Int, item: ItemDef): Boolean = item.count(this) >= count
}

object Player {
  type Perform = PartialFunction[Any, Control[Unit]]

  final case class MoveTrampoline(delay: Int)

  sealed abstract class PlayState
  object PlayState {
    case object Playing extends PlayState
    case object Won extends PlayState
    case object Lost extends PlayState
  }

  trait PerPlayerData[+A] {
    def apply(player: Player): A
  }

  object immutable {
    trait PerPlayerData[+A] extends Player.PerPlayerData[A] {
      private val data = MutableMap.empty[Player, A @uncheckedVariance]

      def apply(player: Player): A =
        data.getOrElseUpdate(player, initial(player))

      protected def initial(player: Player): A
    }

    class SimplePerPlayerData[+A](default: Player => A) extends PerPlayerData[A] {
      def this(default: A) = this(_ => default)

      protected def initial(player: Player) = default(player)
    }
  }

  object mutable {
    trait PerPlayerData[A] extends Player.PerPlayerData[A] {
      private val data = MutableMap.empty[Player, A]

      def apply(player: Player): A =
        data.getOrElseUpdate(player, initial(player))
      def update(player: Player, value: A): Unit =
        data.put(player, value)

      protected def initial(player: Player): A
    }

    class SimplePerPlayerData[A](default: Player => A) extends PerPlayerData[A] {
      def this(default: A) = this(_ => default)

      protected def initial(player: Player) = default(player)
    }
  }
}
