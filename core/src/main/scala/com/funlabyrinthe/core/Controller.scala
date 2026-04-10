package com.funlabyrinthe.core

import graphics._
import input._
import indigo.SceneUpdateFragment

trait Controller {
  def viewSize: (Double, Double)

  def drawView(context: DrawContext): Unit = {
    graphics.fillWithOpaqueBackground(context)
  }

  def present(): SceneUpdateFragment

  def onKeyEvent(keyEvent: KeyEvent): Unit = ()
}

object Controller {
  object Dummy extends Controller {
    def viewSize = (270.0, 270.0) // the everlasting default view size

    def present(): SceneUpdateFragment = SceneUpdateFragment.empty
  }
}
