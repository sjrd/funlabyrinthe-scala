package com.funlabyrinthe.core

import input._
import scene.*

trait Controller {
  def viewSize: Size

  def present(): SceneUpdateFragment

  def onKeyEvent(keyEvent: KeyEvent): Unit = ()
}

object Controller {
  object Dummy extends Controller {
    def viewSize = Size(270, 270) // the everlasting default view size

    def present(): SceneUpdateFragment = SceneUpdateFragment.empty
  }
}
