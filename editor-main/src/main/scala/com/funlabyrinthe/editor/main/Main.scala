package com.funlabyrinthe.editor.main

import scala.scalajs.js
import scala.scalajs.js.JSConverters.*

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import node.*

import com.funlabyrinthe.editor.common.CompilerService
import com.funlabyrinthe.editor.common.FileService

import com.funlabyrinthe.editor.main.electron.{app, BrowserWindow}
import com.funlabyrinthe.editor.main.electron.BrowserWindow.WebPreferences
import com.funlabyrinthe.editor.main.electron.ipcMain
import com.funlabyrinthe.editor.main.electron.dialog
import com.funlabyrinthe.editor.main.electron.dialog.FileFilter

object Main:
  private val ScalaVersion = "3.3.0"

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
      PreloadScriptGenerator.generateFor[CompilerService]("compilerService"),
    )

    val fileName = path.join(path.__dirname, "..", "preload.js")

    fsPromises.writeFile(fileName, contents, "utf-8").toFuture.map(_ => fileName)
  end generatePreloadScript

  private def setupIPCHandlers(window: BrowserWindow): Unit =
    val libsDir = path.join(path.__dirname, "..", "..", "libs")
    val libs = fsPromises.readdir(libsDir).`then`(_.map(lib => standardizePath(path.join(libsDir, lib))))

    val fileService = new FileServiceImpl(window, libs)
    val compilerService = new CompilerServiceImpl()

    PreloadScriptGenerator.registerHandler[FileService]("fileService", fileService)
    PreloadScriptGenerator.registerHandler[CompilerService]("compilerService", compilerService)
  end setupIPCHandlers

  private class FileServiceImpl(window: BrowserWindow, libs: js.Promise[js.Array[String]]) extends FileService:
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
      }).`then`(_ => ())
    end createDirectories
  end FileServiceImpl

  private class CompilerServiceImpl() extends CompilerService:
    def compileProject(
      sourceDir: String,
      targetDir: String,
      classpath: js.Array[String]
    ): js.Promise[CompilerService.Result] =
      val child = childProcess.spawn(
        "scala-cli",
        js.Array(
          "compile",
          "--scala",
          ScalaVersion,
          "-cp",
          classpath.mkString(";"),
          "-d",
          targetDir,
          ".",
        ),
        new {
          cwd = sourceDir
          stdio = js.Array("ignore", "pipe", "pipe")
        }
      )

      val fullOutput = new java.lang.StringBuilder()
      for readable <- List(child.stdout, child.stderr) do
        readable.setEncoding("utf-8")
        readable.on("data") { str =>
          fullOutput.append(str)
        }
      end for

      new js.Promise[CompilerService.Result]({ (resolve, reject) =>
        child.on("error") { err =>
          reject(new js.Error(s"Compilation process could not start; is scala-cli installed?\n$err"))
        }
        child.on("close") { (exitCode, signal) =>
          val result = new CompilerService.Result {
            val logLines = fullOutput.toString().linesIterator.toJSArray
            val success = exitCode == 0
          }
          resolve(result)
        }
      })
    end compileProject
  end CompilerServiceImpl

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
