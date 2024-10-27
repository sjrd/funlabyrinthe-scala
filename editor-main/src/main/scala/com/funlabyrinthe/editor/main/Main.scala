package com.funlabyrinthe.editor.main

import scala.scalajs.js
import scala.scalajs.js.JSConverters.*

import java.io.{PrintWriter, StringWriter}

import scala.concurrent.{Future, Promise}
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

import org.scalajs.linker.interface.{IRFileCache, Linker, ModuleKind}
import org.scalajs.linker.{NodeIRContainer, NodeOutputDirectory}
import org.scalajs.logging.{Level, Logger}

object Main:
  private val ScalaVersion = "3.5.1"
  private val ScalaJSVersion = "1.17.0"

  private val ImageFilters: js.Array[FileFilter] =
    js.Array(
      new FileFilter {
        val name = "Images"
        val extensions = js.Array("png", "gif")
      }
    )
  end ImageFilters

  def main(args: Array[String]): Unit =
    val preloadScriptFut = generatePreloadScript()
    app.commandLine.appendSwitch(
      "--js-flags",
      "--experimental-wasm-exnref --experimental-wasm-imported-strings --experimental-wasm-jspi --turboshaft-wasm"
    )
    for
      _ <- app.whenReady().toFuture
      preloadScript <- preloadScriptFut
    do
      val window = createWindow(preloadScript)
      setupIPCHandlers(window)
  end main

  private def dirname: String =
    js.Dynamic.global.__dirname.asInstanceOf[String]

  private def generatePreloadScript(): Future[String] =
    val contents = PreloadScriptGenerator.compose(
      PreloadScriptGenerator.generateFor[FileService]("fileService"),
      PreloadScriptGenerator.generateFor[CompilerService]("compilerService"),
    )

    val fileName = pathMod.join(dirname, "..", "preload.js")

    fsPromisesMod.writeFile(fileName, contents, BufferEncoding.utf8).toFuture.map(_ => fileName)
  end generatePreloadScript

  private def setupIPCHandlers(window: BrowserWindow): Unit =
    val libsDir = pathMod.join(dirname, "..", "..", "libs")
    val libs = fsPromisesMod.readdir(libsDir).`then`(_.map(lib => standardizePath(pathMod.join(libsDir, lib))))

    val fileService = new FileServiceImpl(window, libs)
    val compilerService = new CompilerServiceImpl()

    PreloadScriptGenerator.registerHandler[FileService]("fileService", fileService)
    PreloadScriptGenerator.registerHandler[CompilerService]("compilerService", compilerService)
  end setupIPCHandlers

  private class FileServiceImpl(window: BrowserWindow, libs: js.Promise[js.Array[String]]) extends FileService:
    def funlabyCoreLibs(): js.Promise[js.Array[String]] =
      libs

    def showOpenImageDialog(): js.Promise[js.UndefOr[String]] =
      val resultPromise = dialog.showOpenDialog(window, new {
        filters = ImageFilters
      })
      resultPromise.`then`(_.filePaths.headOption.filter(_ != "").map(standardizePath(_)).orUndefined)
    end showOpenImageDialog

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

    def createNewProject(projectName: String): js.Promise[String] =
      val dir = typings.node.osMod.homedir() + "/FunLabyDocuments"
      val projectDir = dir + "/" + projectName
      fsPromisesMod.mkdir(projectDir, new MakeDirectoryOptions {
        recursive = true
      }).`then`(_ => projectDir)
    end createNewProject
  end FileServiceImpl

  private class CompilerServiceImpl() extends CompilerService:
    def compileProject(
      projectDir: String,
      dependencyClasspath: js.Array[String],
      fullClasspath: js.Array[String]
    ): js.Promise[CompilerService.Result] =
      val sourceDir = projectDir + "/Sources"
      val targetDir = projectDir + "/Target"

      val command = List(
        "scala-cli",
        "--power",
        "compile",
        "--js",
        "--scala",
        ScalaVersion,
        "--js-version",
        ScalaJSVersion,
        "-cp",
        dependencyClasspath.mkString(";"),
        "-d",
        targetDir,
        ".",
      )

      println(command.mkString(" "))

      val child = childProcessMod.spawn(
        command.head,
        command.tail.toJSArray,
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

      def traceToOutput(th: Throwable): Unit =
        val sw = new StringWriter()
        th.printStackTrace(new PrintWriter(sw))
        fullOutput.append(sw.toString())
      end traceToOutput

      val scalaCLIResult: Future[Unit] =
        val p = Promise[Unit]()
        child.on("error", { err =>
          p.failure(new Exception(s"Compilation process could not start; is scala-cli installed?\n$err"))
        })
        child.on("close", { exitCode =>
          if exitCode == 0 then
            p.success(())
          else
            p.failure(new Exception(s"Compilation process exited with an error: $exitCode"))
        })
        p.future
      end scalaCLIResult

      val logger = new Logger {
        def log(level: Level, message: => String): Unit =
          if level >= Level.Debug then
            fullOutput.append(s"[$level] $message\n")

        def trace(t: => Throwable): Unit =
          traceToOutput(t)
      }

      val modClassNamesFut: Future[List[String]] = for
        _ <- scalaCLIResult
        modClassNames <- findAllModules(fullClasspath.toList)
        report <- link(fullClasspath.toList, projectDir + "/runtime-under-test", logger)
      yield
        modClassNames
      end modClassNamesFut

      modClassNamesFut
        .map(x => (true, x))
        .recover {
          case th: Throwable =>
            traceToOutput(th)
            (false, Nil)
        }
        .map { (success0, modClassNames0) =>
          val logLines0 = fullOutput.toString().linesIterator.toJSArray
          new CompilerService.Result {
            val logLines = logLines0
            val success = success0
            val moduleClassNames = modClassNames0.toJSArray
          }
        }
        .toJSPromise
    end compileProject

    private def findAllModules(fullClasspath: List[String]): Future[List[String]] =
      import tastyquery.Classpaths.*
      import tastyquery.Contexts.*
      import tastyquery.Symbols.*

      for cp <- tastyquery.nodejs.ClasspathLoaders.read(fullClasspath) yield
        val ctx = Context.initialize(cp)

        given Context = ctx

        val ModuleClass = ctx.findTopLevelClass("com.funlabyrinthe.core.Module")
        val builder = List.newBuilder[String]

        for (entry, entryFile) <- cp.zip(fullClasspath) do
          // Ignore some of the largest irrelevant dependencies
          val packages = entry.listAllPackages()
          val ignore =
            packages.isEmpty
              || entryFile.endsWith("extracted-rt.jar")
              || entryFile.contains("scala-library")
              || entryFile.contains("scala3-library")
              || entryFile.contains("scalajs-javalib")
              || entryFile.contains("scalajs-library")
          if !ignore then
            println(entryFile)
            println(packages.toList.map(_.dotSeparatedName))
            for case cls: ClassSymbol <- ctx.findSymbolsByClasspathEntry(entry) do
              if cls.isModuleClass && cls.isSubClass(ModuleClass) then
                builder += cls.displayFullName.stripSuffix("$")
        end for

        builder.result()
    end findAllModules

    private lazy val globalIRCache: IRFileCache =
      new org.scalajs.linker.standard.StandardIRFileCache()

    private lazy val linker: Linker =
      val config = org.scalajs.linker.interface.StandardConfig()
        .withModuleKind(ModuleKind.ESModule)
        .withExperimentalUseWebAssembly(true)
      org.scalajs.linker.StandardImpl.linker(config)
    end linker

    private def link(fullClasspath: List[String], outputDir: String, logger: Logger): Future[Unit] =
      val cache = globalIRCache.newCache
      val output = NodeOutputDirectory(outputDir)
      val result: Future[Unit] =
        for
          _ <- fsPromisesMod.mkdir(outputDir, new MakeDirectoryOptions {
            recursive = true
          }).toFuture
          (irContainers, _) <- NodeIRContainer.fromClasspath(fullClasspath)
          irFiles <- cache.cached(irContainers)
          report <- linker.link(irFiles, moduleInitializers = Nil, output, logger)
          _ <- patchLoaderFile(outputDir + "/__loader.js")
          _ <- patchForJSPIHack(outputDir + "/main.js")
        yield
          logger.info(s"Successfully linked to $outputDir")
      result.andThen(_ => cache.free())
    end link

    private def patchLoaderFile(file: String): Future[Unit] =
      patchFile(file) { content =>
        content.replace("if (resolvedURL.protocol === 'file:')", "if (false)")
      }
    end patchLoaderFile

    private def patchForJSPIHack(file: String): Future[Unit] =
      patchFile(file) { content =>
        content
          .replace("((x) => magicJSPIAwait(x))", "new WebAssembly.Suspending((x) => x)") // import
          .replace("((f) => (function(arg) {\n    return f(arg);\n  }))", "((f) => WebAssembly.promising(f))") // export
      }
    end patchForJSPIHack

    private def patchFile(file: String)(patch: String => String): Future[Unit] =
      for
        content <- fsPromisesMod.readFile(file, BufferEncoding.utf8).toFuture
        _ <- fsPromisesMod.writeFile(file, patch(content), BufferEncoding.utf8).toFuture
      yield
        ()
    end patchFile
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
