package com.funlabyrinthe.editor.main

import scala.scalajs.js
import scala.scalajs.js.JSConverters.*

import scala.concurrent.ExecutionContext.Implicits.global

import node.path

import Electron.{app, BrowserWindow, WebPreferences}

object Main:
  def main(args: Array[String]): Unit =
    println("Hello from Electron")
    for _ <- app.whenReady().toFuture do
      createWindow()
  end main

  def createWindow(): Unit =
    val win = new BrowserWindow(new {
      width = 800
      height = 600
      show = false
      webPreferences = new WebPreferences {
        preload = path.join(path.__dirname, "..", "..", "..", "preload.js")
      }
    })
    win.loadFile("./editor-renderer/index.html")
    win.maximize()
    win.show() // for focus
  end createWindow
end Main
