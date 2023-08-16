package com.funlabyrinthe.editor.main.electron

import scala.scalajs.js
import scala.scalajs.js.annotation.*

@js.native
@JSImport("electron")
object app extends js.Object {
  def whenReady(): js.Promise[Any] = js.native
}
