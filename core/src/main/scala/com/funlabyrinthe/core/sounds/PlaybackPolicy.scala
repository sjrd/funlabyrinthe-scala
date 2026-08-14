package com.funlabyrinthe.core.sounds

enum PlaybackPolicy {

  /** Stop all sounds, not only the previous same sound. */
  case StopAll

  /** Stop only the previous same sound. */
  case StopPreviousSame

  /** Continue all previous sounds. */
  case Continue
}
