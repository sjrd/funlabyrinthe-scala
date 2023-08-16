package com.funlabyrinthe.editor.main.electron

import scala.scalajs.js
import scala.scalajs.js.annotation.*

@js.native
@JSImport("electron")
object ipcMain extends js.Object:
  def handle(channel: String, handler: js.Function): Unit = js.native
end ipcMain
