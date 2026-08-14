package com.funlabyrinthe.core.sounds

import com.funlabyrinthe.core.pickling.Pickleable
import com.funlabyrinthe.core.inspecting.Inspectable

/** Volume for audio.
 *
 *  The value should be in the range 0.0 (minimum) to 1.0 (maximum).
 */
final case class Volume(value: Double) derives Pickleable, Inspectable

object Volume {
  val Min: Volume = Volume(0.0)
  val Max: Volume = Volume(1.0)
}
