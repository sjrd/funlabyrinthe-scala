package com.funlabyrinthe.editor.main.electron

import scala.scalajs.js
import scala.scalajs.js.annotation.*

@js.native
@JSImport("electron")
object app extends js.Object {
  val commandLine: CommandLine = js.native

  def whenReady(): js.Promise[Any] = js.native
}
