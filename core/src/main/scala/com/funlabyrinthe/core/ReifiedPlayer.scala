package com.funlabyrinthe.core

import scala.collection.immutable.TreeSet

import com.funlabyrinthe.core.CorePlayer.PlayState

/** Base trait for mode-specific player extensions.
 *
 *  It forwards useful methods to an underlying `CorePlayer`.
 */
trait ReifiedPlayer extends Component:
  @transient
  val corePlayer: CorePlayer

  @transient @noinspect
  def playState: PlayState = corePlayer.playState

  @transient
  def plugins: TreeSet[CorePlayerPlugin] = corePlayer.plugins

  def plugins_=(value: TreeSet[CorePlayerPlugin]): Unit = corePlayer.plugins = value

  @transient @noinspect
  val attributes: AttributeBag = corePlayer.attributes

  category = ComponentCategory("players", "Players")

  protected def autoProvideController(): Option[Controller] = None

  export corePlayer.{
    sleep,
    waitForKeyEvent,
    isAbleTo,
    perform,
    tryPerform,
    dispatch,
    win,
    lose,
    showMessage,
    showSelectionMessage,
    showSelectNumberMessage,
    can,
    cannot,
    has,
  }
end ReifiedPlayer

object ReifiedPlayer:
  type Factory[A <: ReifiedPlayer] = ComponentInit ?=> CorePlayer => A

  private[core] def autoProvideController(reifiedPlayer: ReifiedPlayer): Option[Controller] =
    reifiedPlayer.autoProvideController()
end ReifiedPlayer
