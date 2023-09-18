package com.funlabyrinthe.core

import cps.customValueDiscard

import input.KeyEvent

import scala.collection.immutable.TreeSet
import scala.collection.mutable.{ Map => MutableMap }
import scala.reflect.{ClassTag, classTag}

final class CorePlayer private[core] (using ComponentInit) extends Component:
  import universe.*
  import CorePlayer.*

  icon += "Pawns/Player"

  var playState: PlayState = PlayState.Playing
  var plugins: TreeSet[CorePlayerPlugin] = TreeSet.empty
  var controller: Controller = Controller.Dummy

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

  // Actions

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

  // Message dispatch

  def dispatch[A](message: Message[A]): Control[A] =
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

  def win(): Unit = {
    playState = PlayState.Won

    for (player <- components[CorePlayer]; if player ne this)
      player.playState = PlayState.Lost

    universe.terminate()
  }

  def lose(): Unit = {
    playState = PlayState.Lost

    if (components[CorePlayer].forall(_.playState != PlayState.Playing))
      universe.terminate()
  }

  // Messages

  def showMessage(message: String): Control[Unit] =
    messages.MessageOps.showMessage(this, message)

  def showSelectionMessage(
    prompt: String,
    answers: List[String],
    default: Int = 0,
    showOnlySelected: Boolean = false,
  ): Control[Int] =
    messages.MessageOps.showSelectionMessage(this, prompt, answers, default, showOnlySelected)
  end showSelectionMessage

  def showSelectNumberMessage(
    prompt: String,
    min: Int,
    max: Int,
    default: Int = Int.MinValue,
  ): Control[Int] =
    messages.MessageOps.showSelectNumberMessage(this, prompt, min, max, default)
  end showSelectNumberMessage

  // DSL
  def can(action: Any): Control[Boolean] = tryPerform(action)
  def cannot(action: Any): Control[Boolean] = tryPerform(action).map(!_)

  def has(item: ItemDef): Boolean = item.count(this) > 0
  def has(count: Int, item: ItemDef): Boolean = item.count(this) >= count
end CorePlayer

object CorePlayer:
  type Perform = PartialFunction[Any, Control[Unit]]

  final case class MoveTrampoline(delay: Int)

  enum PlayState:
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
