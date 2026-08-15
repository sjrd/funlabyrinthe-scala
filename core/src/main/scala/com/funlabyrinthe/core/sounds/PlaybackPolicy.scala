package com.funlabyrinthe.core.sounds

import com.funlabyrinthe.core.pickling.Pickleable
import com.funlabyrinthe.core.inspecting.Inspectable

enum PlaybackPolicy derives Pickleable, Inspectable {

  /** Continue all previous sounds. */
  case Continue

  /** Stop all sounds, not only the previous same sound. */
  case StopAll

  /** Stop only the previous same sound. */
  case StopPreviousSame
}
