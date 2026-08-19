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

  @transient @noinspect
  def isPlaying: Boolean = corePlayer.isPlaying

  @transient
  def plugins: TreeSet[CorePlayerPlugin] = corePlayer.plugins

  def plugins_=(value: TreeSet[CorePlayerPlugin]): Unit = corePlayer.plugins = value

  @transient @noinspect
  val attributes: AttributeBag = corePlayer.attributes

  @transient @noinspect
  object items {
    def apply(item: ItemDef): Int = corePlayer.items(item)
    def update(item: ItemDef, count: Int): Unit = corePlayer.items(item) = count
  }

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
    showMessageOnce,
    showSelectionMessage,
    showSelectNumberMessage,
    playSound,
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
