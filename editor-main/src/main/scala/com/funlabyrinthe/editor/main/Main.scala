package com.funlabyrinthe.editor.main

import scala.scalajs.js
import scala.scalajs.js.JSConverters.*

import java.io.{PrintWriter, StringWriter}

import scala.concurrent.{Future, Promise}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Success

import com.funlabyrinthe.editor.common.CompilerService
import com.funlabyrinthe.editor.common.FileService

import com.funlabyrinthe.editor.main.electron.{app, BrowserWindow}
import com.funlabyrinthe.editor.main.electron.BrowserWindow.WebPreferences
import com.funlabyrinthe.editor.main.electron.ipcMain
import com.funlabyrinthe.editor.main.electron.dialog
import com.funlabyrinthe.editor.main.electron.dialog.FileFilter

import typings.node.anon.ObjectEncodingOptionswithEncoding
import typings.node.bufferMod.global.BufferEncoding
import typings.node.childProcessMod
import typings.node.childProcessMod.{IOType, SpawnOptions}
import typings.node.fsMod.MakeDirectoryOptions
import typings.node.fsPromisesMod
import typings.node.nodeBooleans
import typings.node.nodeColonfsMod.Dirent
import typings.node.pathMod

import org.scalajs.linker.interface.{IRFileCache, Linker, ModuleKind}
import org.scalajs.linker.{NodeIRContainer, NodeOutputDirectory}
import org.scalajs.logging.{Level, Logger}

object Main:
  private val ScalaVersion = "3.5.1"
  private val ScalaJSVersion = "1.17.0"

  private val ScalaLibraryName = raw"""/(?:scala-library|scala3-library_3)-[.0-9]+\.jar$$""".r
  private val coreBridgeModulePath =
    "./../../../../core-bridge/target/scala-3.5.1/funlaby-core-bridge-fastopt/main.js"

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

  private def documentsDir(): String =
    typings.node.osMod.homedir() + "/FunLabyDocuments"

  private def projectDirFor(projectID: String): String =
    documentsDir() + "/" + projectID

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
    val libs = fsPromisesMod.readdir(libsDir).toFuture.map { libFiles =>
      libFiles.toList.map(lib => standardizePath(pathMod.join(libsDir, lib)))
    }

    val fileService = new FileServiceImpl(window)
    val compilerService = new CompilerServiceImpl(libs)

    PreloadScriptGenerator.registerHandler[FileService]("fileService", fileService)
    PreloadScriptGenerator.registerHandler[CompilerService]("compilerService", compilerService)
  end setupIPCHandlers

  private def mkdirRecursive(dir: String): Future[Unit] =
    fsPromisesMod.mkdir(dir, new MakeDirectoryOptions {
      recursive = true
    }).toFuture.map(_ => ())
  end mkdirRecursive

  private def fileExists(file: String): Future[Boolean] =
    fsPromisesMod.access(file).toFuture.transform(s => Success(s.isSuccess))

  private def readdirWithFileTypes(dir: String): Future[js.Array[Dirent]] =
    fsPromisesMod
      .readdir(dir, new ObjectEncodingOptionswithEncoding {
        var withFileTypes = nodeBooleans.`true`
      })
      .toFuture
  end readdirWithFileTypes

  private def readdirWithEntryFilter(dir: String)(p: Dirent => Boolean): Future[js.Array[String]] =
    readdirWithFileTypes(dir).map { entries =>
      entries.filter(p).map(_.name)
    }
  end readdirWithEntryFilter

  private class FileServiceImpl(window: BrowserWindow) extends FileService:
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

    def listAvailableProjects(): js.Promise[js.Array[FileService.ProjectDef]] =
      def listSubDirs(dir: String): Future[Seq[String]] =
        readdirWithEntryFilter(dir)(_.isDirectory()).map(_.toSeq)

      def tryReadFile(file: String): Future[Option[String]] =
        fsPromisesMod.readFile(file, BufferEncoding.utf8).toFuture
          .map(Some(_))
          .recover(_ => None)

      val dir = documentsDir()

      val projectIDsByOwnerIDsFuture: Future[Seq[(String, Seq[String])]] = for
        ownerIDs <- listSubDirs(dir)
        projectIDsByOwnerIDs <- Future.sequence(ownerIDs.map(o => listSubDirs(s"$dir/$o").map(o -> _)))
      yield
        projectIDsByOwnerIDs

      val projectIDsFuture: Future[Seq[String]] =
        projectIDsByOwnerIDsFuture.map { projectIDsByOwnerIDs =>
          projectIDsByOwnerIDs.flatMap { (ownerID, simpleProjectIDs) =>
            simpleProjectIDs.map(simpleProjectID => s"$ownerID/$simpleProjectID")
          }
        }

      val projectDefsFuture: Future[Seq[FileService.ProjectDef]] =
        projectIDsFuture.flatMap { projectIDs =>
          val withMaybeFileContent = Future.traverse(projectIDs) { projectID =>
            for
              fileContent <- tryReadFile(s"$dir/$projectID/project.json")
            yield
              (projectID, fileContent)
          }
          withMaybeFileContent.map(_.collect {
            case (projectID, Some(fileContent)) =>
              new FileService.ProjectDef {
                val id: String = projectID
                val projectFileContent: String = fileContent
              }
          })
        }

      projectDefsFuture.map(_.toJSArray).toJSPromise
    end listAvailableProjects

    def createNewProject(
        projectID: String): js.Promise[js.Tuple2[FileService.ProjectDef, FileService.ProjectLoadInfo]] =

      val projectDir = projectDirFor(projectID)
      fsPromisesMod.mkdir(projectDir, new MakeDirectoryOptions {
        recursive = true
      }).`then` { _ =>
        val projectDef = new FileService.ProjectDef {
          val id = projectID
          val projectFileContent: String = "{}"
        }
        val loadInfo = new FileService.ProjectLoadInfo {
          val runtimeURI = coreBridgeModulePath
          val universeFileContent = "{}"
          val sourceFiles = js.Array[String]()
        }
        js.Tuple2(projectDef, loadInfo)
      }
    end createNewProject

    def loadProject(projectID: String): js.Promise[FileService.ProjectLoadInfo] =
      val projectDir = projectDirFor(projectID)
      val runtimeFile = s"$projectDir/runtime-under-test/main.js"
      val universeFile = s"$projectDir/universe.json"

      val sourceFilesFuture: Future[js.Array[String]] =
        readdirWithEntryFilter(s"$projectDir/sources")(_.isFile()).recover(_ => js.Array())

      val future = for
        runtimeFileExists <- fileExists(runtimeFile)
        universeFileContent0 <- readFileToString(universeFile).toFuture
        sourceFiles0 <- sourceFilesFuture
      yield
        val runtimeURI0 =
          if runtimeFileExists then runtimeFile
          else coreBridgeModulePath

        new FileService.ProjectLoadInfo {
          val runtimeURI = runtimeURI0
          val universeFileContent = universeFileContent0
          val sourceFiles = sourceFiles0
        }
      end future

      future.toJSPromise
    end loadProject

    def saveProject(projectID: String, projectFileContent: String,
        universeFileContent: String): js.Promise[Unit] =

      val projectDir = projectDirFor(projectID)
      writeStringToFile(s"$projectDir/project.json", projectFileContent)
        .`then`(_ => writeStringToFile(s"$projectDir/universe.json", universeFileContent))
    end saveProject

    def loadSourceFile(projectID: String, sourceFile: String): js.Promise[String] =
      val sourcesDir = projectDirFor(projectID) + "/sources"
      readFileToString(s"$sourcesDir/$sourceFile")
    end loadSourceFile

    def saveSourceFile(projectID: String, sourceFile: String, content: String): js.Promise[Unit] =
      val sourcesDir = projectDirFor(projectID) + "/sources"
      mkdirRecursive(sourcesDir).flatMap { _ =>
        writeStringToFile(s"$sourcesDir/$sourceFile", content).toFuture
      }.toJSPromise
    end saveSourceFile
  end FileServiceImpl

  private class CompilerServiceImpl(coreLibsFuture: Future[List[String]])
      extends CompilerService:

    def compileProject(projectID: String): js.Promise[CompilerService.Result] =
      val projectDir = projectDirFor(projectID)

      val sourceDir = projectDir + "/sources"
      val targetDir = projectDir + "/target"

      val fullOutput = new java.lang.StringBuilder()

      def traceToOutput(th: Throwable): Unit =
        val sw = new StringWriter()
        th.printStackTrace(new PrintWriter(sw))
        fullOutput.append(sw.toString())
      end traceToOutput

      def runScalaCLI(dependencyClasspath: List[String]): Future[Unit] =
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

        for readable <- List(child.stdout, child.stderr) do
          readable.asInstanceOf[js.Dynamic].setEncoding("utf-8")
          readable.asInstanceOf[js.Dynamic].on("data", { (str: String) =>
            fullOutput.append(str)
          })
        end for

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
      end runScalaCLI

      val logger = new Logger {
        def log(level: Level, message: => String): Unit =
          if level >= Level.Debug then
            fullOutput.append(s"[$level] $message\n")

        def trace(t: => Throwable): Unit =
          traceToOutput(t)
      }

      val modClassNamesFut: Future[List[String]] = for
        coreLibs <- coreLibsFuture
        dependencyClasspath = coreLibs.filter(!ScalaLibraryName.matches(_))
        fullClasspath = coreLibs :+ targetDir
        _ <- mkdirRecursive(sourceDir)
        _ <- mkdirRecursive(targetDir)
        _ <- runScalaCLI(dependencyClasspath)
        modClassNames <- findAllModules(fullClasspath)
        report <- link(fullClasspath, projectDir + "/runtime-under-test", logger)
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
          _ <- mkdirRecursive(outputDir)
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
