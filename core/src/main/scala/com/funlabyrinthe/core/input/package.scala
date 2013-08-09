package com.funlabyrinthe.core

import scalafx.scene.{ input => sfxsi }

package object input {
  type KeyEvent = sfxsi.KeyEvent
  lazy val KeyEvent = sfxsi.KeyEvent

  type MouseEvent = sfxsi.MouseEvent
  lazy val MouseEvent = sfxsi.MouseEvent

  def hasAnyControlKey(event: KeyEvent): Boolean =
    event.controlDown || event.altDown || event.shiftDown || event.metaDown
}
