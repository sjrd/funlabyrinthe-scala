package com.funlabyrinthe.core

import graphics._
import input._

trait Controller {
  def viewSize: (Double, Double)

  def drawView(context: DrawContext): Unit = {
    graphics.fillWithOpaqueBackground(context)
  }

  def onKeyEvent(keyEvent: KeyEvent): Unit @control = ()
}

object Controller {
  object Dummy extends Controller {
    def viewSize = (270.0, 270.0) // the everlasting default view size
  }
}
