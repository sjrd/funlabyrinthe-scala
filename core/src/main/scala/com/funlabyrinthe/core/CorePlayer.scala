package com.funlabyrinthe.core

import com.funlabyrinthe.core.input.KeyEvent
import com.funlabyrinthe.core.pickling.Pickleable

import scala.collection.immutable.TreeSet
import scala.collection.mutable.{ Map => MutableMap }
import scala.reflect.{ClassTag, classTag}

final class CorePlayer private[core] (using ComponentInit) extends Component:
  import universe.*
  import CorePlayer.*

  category = ComponentCategory("players", "Players")

  icon += "Pawns/Player"

  @noinspect
  var playState: PlayState = PlayState.Playing

  var plugins: TreeSet[CorePlayerPlugin] = TreeSet.empty

  @transient @noinspect // FIXME We actually need to persist this somehow
  var controller: Controller = Controller.Dummy

  @noinspect
  val attributes: AttributeBag = new AttributeBag

  @transient @noinspect
  private var controlHandler: ControlHandler = ControlHandler.Uninitialized

  private val reifiedPlayers = collection.mutable.LinkedHashMap.empty[Class[? <: ReifiedPlayer], ReifiedPlayer]

  private[core] def registerReified[A <: ReifiedPlayer](cls: Class[A], reified: A): Unit =
    reifiedPlayers += cls -> reified

  def reified[A <: ReifiedPlayer](using ClassTag[A]): A =
    val cls = classTag[A].runtimeClass.asInstanceOf[Class[A]]
    cls.cast(reifiedPlayers.getOrElse(cls, {
      throw IllegalArgumentException(s"Unknown reified player class ${cls.getName()}")
    }))
  end reified

  def autoDetectController(): Boolean =
    val iter = reifiedPlayers.valuesIterator.flatMap(ReifiedPlayer.autoProvideController(_))
    if iter.hasNext then
      controller = iter.next()
      true
    else
      false
  end autoDetectController

  private[funlabyrinthe] def setControlHandler(handler: ControlHandler): Unit =
    controlHandler = handler

  // Control

  def sleep(ms: Int): Unit =
    controlHandler.sleep(ms)

  def waitForKeyEvent(): KeyEvent =
    controlHandler.waitForKeyEvent()

  def enqueueUnderControl(op: () => Unit): Unit =
    controlHandler.enqueueUnderControl(op)

  // Actions

  def isAbleTo(ability: Ability): Boolean = {
    (plugins.exists(p => p.perform(this).isDefinedAt(ability)) ||
        ItemDef.all.exists(i => i.perform(this).isDefinedAt(ability)))
  }

  def perform(ability: Ability): Unit = {
    if (!tryPerform(ability))
      assert(false, "must not call perform(ability) if !isAbleTo(ability)")
  }

  def tryPerform(ability: Ability): Boolean = {
    val perform = {
      plugins.collectFirst({
        case p if p.perform(this).isDefinedAt(ability) => p.perform(this)
      }) orElse ItemDef.all.collectFirst({
        case i if i.perform(this).isDefinedAt(ability) => i.perform(this)
      })
    }

    if (perform.isDefined) {
      perform.get(ability)
      true
    } else {
      false
    }
  }

  // Message dispatch

  private[core] def canDispatch[A](message: Message[A]): Boolean =
    plugins.exists(_.onMessage[A](this).isDefinedAt(message))

  def dispatch[A](message: Message[A]): A =
    plugins.iterator
      .map(_.onMessage[A](this))
      .collectFirst {
        case pf if pf.isDefinedAt(message) => pf(message)
      }
      .getOrElse {
        throw IllegalArgumentException(s"The player '$this' has no plugin that can handle the message $message")
      }
  end dispatch

  // Play state

  /** Is the player still playing, i.e., they have not won nor lost yet? */
  @transient @noinspect
  def isPlaying: Boolean = playState == PlayState.Playing

  def win(): Unit = {
    playState = PlayState.Won

    for (player <- components[CorePlayer]; if player ne this)
      player.playState = PlayState.Lost

    universe.terminate()
  }

  def lose(): Unit = {
    playState = PlayState.Lost

    if (components[CorePlayer].forall(!_.isPlaying))
      universe.terminate()
  }

  // Messages

  def showMessage(message: String): Unit =
    messages.MessageOps.showMessage(this, message)

  def showSelectionMessage(
    prompt: String,
    answers: List[String],
    default: Int = 0,
    showOnlySelected: Boolean = false,
  ): Int =
    messages.MessageOps.showSelectionMessage(this, prompt, answers, default, showOnlySelected)
  end showSelectionMessage

  def showSelectNumberMessage(
    prompt: String,
    min: Int,
    max: Int,
    default: Int = Int.MinValue,
  ): Int =
    messages.MessageOps.showSelectNumberMessage(this, prompt, min, max, default)
  end showSelectNumberMessage

  // DSL
  infix def can(ability: Ability): Boolean = tryPerform(ability)
  infix def cannot(ability: Ability): Boolean = !tryPerform(ability)

  infix def has(item: ItemDef): Boolean = item.count(this) > 0
  def has(count: Int, item: ItemDef): Boolean = item.count(this) >= count
end CorePlayer

object CorePlayer:
  type Perform = PartialFunction[Ability, Unit]

  final case class MoveTrampoline(delay: Int)

  enum PlayState derives Pickleable:
    case Playing, Won, Lost

  trait PerPlayerData[+A] {
    def apply(player: CorePlayer): A
  }

  object immutable {
    trait PerPlayerData[+A] extends CorePlayer.PerPlayerData[A] {
      private val data = MutableMap.empty[CorePlayer, A]

      def apply(player: CorePlayer): A =
        data.getOrElseUpdate(player, initial(player))

      protected def initial(player: CorePlayer): A
    }

    class SimplePerPlayerData[+A](default: CorePlayer => A) extends PerPlayerData[A] {
      def this(default: A) = this(_ => default)

      protected def initial(player: CorePlayer) = default(player)
    }
  }

  object mutable {
    trait PerPlayerData[A] extends CorePlayer.PerPlayerData[A] {
      private val data = MutableMap.empty[CorePlayer, A]

      def apply(player: CorePlayer): A =
        data.getOrElseUpdate(player, initial(player))
      def update(player: CorePlayer, value: A): Unit =
        data.put(player, value)

      final def apply(player: ReifiedPlayer): A =
        apply(player.corePlayer)
      final def update(player: ReifiedPlayer, value: A): Unit =
        update(player.corePlayer, value)

      protected def initial(player: CorePlayer): A
    }

    class SimplePerPlayerData[A](default: CorePlayer => A) extends PerPlayerData[A] {
      def this(default: A) = this(_ => default)

      protected def initial(player: CorePlayer) = default(player)
    }
  }
end CorePlayer
