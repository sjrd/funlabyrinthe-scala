package com.funlabyrinthe.editor.main

import scala.scalajs.js
import scala.scalajs.js.annotation.*

object Electron:
  @js.native
  @JSImport("electron")
  object app extends js.Object {
    def whenReady(): js.Promise[Any] = js.native
  }

  @js.native
  @JSImport("electron")
  class BrowserWindow(init: BrowserWindowConfig) extends js.Object:
    def loadFile(fileName: String): Unit = js.native
  end BrowserWindow

  trait BrowserWindowConfig extends js.Object:
    var width: js.UndefOr[Int] = js.undefined
    var height: js.UndefOr[Int] = js.undefined
    var webPreferences: js.UndefOr[WebPreferences] = js.undefined
  end BrowserWindowConfig

  trait WebPreferences extends js.Object:
    var preload: js.UndefOr[String] = js.undefined
  end WebPreferences
end Electron
