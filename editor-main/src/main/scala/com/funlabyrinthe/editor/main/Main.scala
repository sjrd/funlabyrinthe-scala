package com.funlabyrinthe.editor.main

import scala.scalajs.js
import scala.scalajs.js.JSConverters.*

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import com.funlabyrinthe.editor.common.CompilerService
import com.funlabyrinthe.editor.common.FileService

import com.funlabyrinthe.editor.main.electron.{app, BrowserWindow}
import com.funlabyrinthe.editor.main.electron.BrowserWindow.WebPreferences
import com.funlabyrinthe.editor.main.electron.ipcMain
import com.funlabyrinthe.editor.main.electron.dialog
import com.funlabyrinthe.editor.main.electron.dialog.FileFilter

import typings.node.bufferMod.global.BufferEncoding
import typings.node.childProcessMod
import typings.node.childProcessMod.{IOType, SpawnOptions}
import typings.node.fsMod.MakeDirectoryOptions
import typings.node.fsPromisesMod
import typings.node.pathMod

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

    val fileName = pathMod.join(typings.node.global.dirname, "..", "preload.js")

    fsPromisesMod.writeFile(fileName, contents, BufferEncoding.utf8).toFuture.map(_ => fileName)
  end generatePreloadScript

  private def setupIPCHandlers(window: BrowserWindow): Unit =
    val libsDir = pathMod.join(typings.node.global.dirname, "..", "..", "libs")
    val libs = fsPromisesMod.readdir(libsDir).`then`(_.map(lib => standardizePath(pathMod.join(libsDir, lib))))

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
      fsPromisesMod.readFile(path, BufferEncoding.utf8)

    def writeStringToFile(path: String, content: String): js.Promise[Unit] =
      fsPromisesMod.writeFile(path, content, BufferEncoding.utf8)

    def createDirectories(path: String): js.Promise[Unit] =
      fsPromisesMod.mkdir(path, new MakeDirectoryOptions {
        recursive = true
      }).`then`(_ => ())
    end createDirectories

    def listAvailableProjects(): js.Promise[js.Array[String]] =
      val dir = typings.node.osMod.homedir() + "/FunLabyDocuments"
      val futureResult = for
        files <- fsPromisesMod.readdir(dir).toFuture
      yield
        files.map(simpleFileName => standardizePath(pathMod.join(dir, simpleFileName)))
      futureResult.toJSPromise
    end listAvailableProjects
  end FileServiceImpl

  private class CompilerServiceImpl() extends CompilerService:
    def compileProject(
      sourceDir: String,
      targetDir: String,
      classpath: js.Array[String]
    ): js.Promise[CompilerService.Result] =
      val child = childProcessMod.spawn(
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
        new SpawnOptions {
          cwd = sourceDir
          stdio = js.Array(IOType.ignore, IOType.pipe, IOType.pipe)
        }
      )

      val fullOutput = new java.lang.StringBuilder()
      for readable <- List(child.stdout, child.stderr) do
        readable.asInstanceOf[js.Dynamic].setEncoding("utf-8")
        readable.asInstanceOf[js.Dynamic].on("data", { (str: String) =>
          fullOutput.append(str)
        })
      end for

      new js.Promise[CompilerService.Result]({ (resolve, reject) =>
        child.on("error", { err =>
          reject(new js.Error(s"Compilation process could not start; is scala-cli installed?\n$err"))
        })
        child.on("close", { (exitCode) =>
          val result = new CompilerService.Result {
            val logLines = fullOutput.toString().linesIterator.toJSArray
            val success = exitCode == 0
          }
          resolve(result)
        })
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
