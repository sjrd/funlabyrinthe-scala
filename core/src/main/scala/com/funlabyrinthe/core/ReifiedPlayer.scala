package com.funlabyrinthe.core

/** Base trait for mode-specific player extensions.
 *
 *  It forwards useful methods to an underlying `CorePlayer`.
 */
trait ReifiedPlayer extends Component:
  val corePlayer: CorePlayer

  protected def autoProvideController(): Option[Controller] = None

  export corePlayer.{
    playState,
    plugins,
    plugins_=,
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
