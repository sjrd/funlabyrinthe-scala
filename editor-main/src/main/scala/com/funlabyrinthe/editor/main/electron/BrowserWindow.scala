package com.funlabyrinthe.editor.main.electron

import scala.scalajs.js
import scala.scalajs.js.annotation.*

@js.native
@JSImport("electron")
class BrowserWindow(init: BrowserWindow.BrowserWindowInit) extends js.Object:
  def loadFile(fileName: String): Unit = js.native

  def maximize(): Unit = js.native
  def show(): Unit = js.native
end BrowserWindow

object BrowserWindow:
  trait BrowserWindowInit extends js.Object:
    var width: js.UndefOr[Int] = js.undefined
    var height: js.UndefOr[Int] = js.undefined
    var show: js.UndefOr[Boolean] = js.undefined
    var webPreferences: js.UndefOr[WebPreferences] = js.undefined
  end BrowserWindowInit

  trait WebPreferences extends js.Object:
    var preload: js.UndefOr[String] = js.undefined
  end WebPreferences
end BrowserWindow
