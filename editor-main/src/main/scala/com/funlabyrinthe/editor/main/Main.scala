package com.funlabyrinthe.editor.main

import scala.scalajs.js
import scala.scalajs.js.JSConverters.*

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import node.*

import com.funlabyrinthe.editor.common.FileService

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
    val preloadScriptFut = generatePreloadScript()
    for
      _ <- app.whenReady().toFuture
      preloadScript <- preloadScriptFut
    do
      val window = createWindow(preloadScript)
      setupIPCHandlers(window)
  end main

  private def generatePreloadScript(): Future[String] =
    val contents = PreloadScriptGenerator.compose(
      PreloadScriptGenerator.generateFor[FileService]("fileService"),
    )

    val fileName = path.join(path.__dirname, "..", "preload.js")

    fsPromises.writeFile(fileName, contents, "utf-8").toFuture.map(_ => fileName)
  end generatePreloadScript

  private def setupIPCHandlers(window: BrowserWindow): Unit =
    val libsDir = path.join(path.__dirname, "..", "..", "libs")
    val libs = fsPromises.readdir(libsDir).`then`(_.map(lib => standardizePath(path.join(libsDir, lib))))

    val service = new FileService {
      def funlabyCoreLibs(): js.Promise[js.Array[String]] =
        libs

      def showOpenProjectDialog(): js.Promise[js.UndefOr[String]] =
        val resultPromise = dialog.showOpenDialog(window, new {
          filters = FunLabyProjectFilters
        })
        resultPromise.`then`(_.filePaths.headOption.filter(_ != "").map(standardizePath(_)).orUndefined)
      end showOpenProjectDialog

      def showSaveNewProjectDialog(): js.Promise[js.UndefOr[String]] =
        val resultPromise = dialog.showSaveDialog(window, new {
          filters = FunLabyProjectFilters
        })
        resultPromise.`then`(_.filePath.filter(_ != "").map(standardizePath(_)))
      end showSaveNewProjectDialog

      def readFileToString(path: String): js.Promise[String] =
        fsPromises.readFile(path, "utf-8")

      def writeStringToFile(path: String, content: String): js.Promise[Unit] =
        fsPromises.writeFile(path, content, "utf-8")

      def createDirectories(path: String): js.Promise[Unit] =
        fsPromises.mkdir(path, new {
          recursive = true
        })
      end createDirectories
    }

    PreloadScriptGenerator.registerHandler[FileService]("fileService", service)
  end setupIPCHandlers

  private def standardizePath(path: String): String =
    path.replace('\\', '/')

  def createWindow(preloadScript: String): BrowserWindow =
    val win = new BrowserWindow(new {
      width = 800
      height = 600
      show = false
      webPreferences = new WebPreferences {
        preload = preloadScript
      }
    })
    win.loadFile("./editor-renderer/index.html")
    win.maximize()
    win.show() // for focus
    win
  end createWindow
end Main
