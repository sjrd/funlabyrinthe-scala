package com.funlabyrinthe.editor.main

import scala.scalajs.js
import scala.scalajs.js.JSConverters.*

import scala.concurrent.ExecutionContext.Implicits.global

import node.path

import com.funlabyrinthe.editor.main.electron.{app, BrowserWindow}
import com.funlabyrinthe.editor.main.electron.BrowserWindow.WebPreferences
import com.funlabyrinthe.editor.main.electron.ipcMain
import com.funlabyrinthe.editor.main.electron.dialog
import com.funlabyrinthe.editor.main.electron.dialog.FileFilter

object Main:
  private val FunLabyProjectFilters: js.Array[FileFilter] =
    js.Array(
      new FileFilter {
        val name = "FunLabyrinthe project"
        val extensions = js.Array("funlaby")
      }
    )
  end FunLabyProjectFilters

  def main(args: Array[String]): Unit =
    for _ <- app.whenReady().toFuture do
      val window = createWindow()
      ipcMain.handle("showSaveNewProjectDialog", { () =>
        val resultPromise = dialog.showSaveDialog(window, new {
          filters = FunLabyProjectFilters
        })
        resultPromise.`then` { (result) =>
          println(result.filePath)
          result.filePath.map(_.replace('\\', '/'))
        }
      })
  end main

  def createWindow(): BrowserWindow =
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
    win
  end createWindow
end Main
