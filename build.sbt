import org.scalajs.linker.interface.ModuleInitializer

val javalibEntry = taskKey[File]("Path to rt.jar or \"jrt:/\"")
val copyCoreLibs = taskKey[Unit]("copy core libs")

val copyTreeSitterFiles = taskKey[Unit]("download and copy tree-sitter files")

inThisBuild(Def.settings(
  scalaVersion := "3.3.0",
  scalacOptions ++= Seq(
      "-deprecation",
      "-unchecked",
      "-feature",
      "-encoding", "utf8",
      "-Xcheck-macros",
  ),
  version := "0.1-SNAPSHOT",
))

val testSettings = Def.settings(
  libraryDependencies += "org.scalameta" %%% "munit" % "0.7.29" % Test,
)

lazy val root = project.in(file("."))
  .settings(
    name := "FunLabyrinthe",
  )
  .aggregate(
    core,
    mazes,
    html5Graphics,
    editorCommon,
    editorMain,
    editorRenderer,
  )

lazy val core = project
  .enablePlugins(ScalaJSPlugin)
  .settings(
    name := "funlaby-core",
    libraryDependencies += "com.github.rssh" %%% "dotty-cps-async" % "0.9.17",
    libraryDependencies += "org.portable-scala" %%% "portable-scala-reflect" % "1.1.2" cross CrossVersion.for3Use2_13,
    testSettings,
  )

lazy val coreInterface = project
  .in(file("core-interface"))
  .enablePlugins(ScalaJSPlugin)
  .settings(
    name := "funlaby-core-interface",
    libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "2.4.0",
  )

lazy val coreBridge = project
  .in(file("core-bridge"))
  .enablePlugins(ScalaJSPlugin)
  .settings(
    name := "funlaby-core-bridge",
    scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.ESModule) },
  )
  .dependsOn(coreInterface, core, mazes, html5Graphics)

lazy val mazes = project
  .enablePlugins(ScalaJSPlugin)
  .settings(
    name := "funlaby-mazes",
  )
  .dependsOn(core)

lazy val html5Graphics = project.in(file("html5-graphics"))
  .enablePlugins(ScalaJSPlugin)
  .settings(
    name := "funlaby-graphics-dom",
    libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "2.4.0",
  )
  .dependsOn(core)

lazy val editorCommon = project
  .in(file("editor-common"))
  .enablePlugins(ScalaJSPlugin)
  .settings(
    name := "funlaby-editor-common",
  )

lazy val editorMain = project
  .in(file("editor-main"))
  .enablePlugins(ScalaJSPlugin, ScalablyTypedConverterExternalNpmPlugin)
  .settings(
    name := "funlaby-editor-main",
    libraryDependencies ++= Seq(
      "ch.epfl.scala" %%% "tasty-query" % "0.9.2",
    ),
    externalNpm := (LocalRootProject / baseDirectory).value,
    // electron does not support ES modules
    scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.CommonJSModule) },
    Compile / scalaJSModuleInitializers ++= Seq(
      ModuleInitializer.mainMethodWithArgs("com.funlabyrinthe.editor.main.Main", "main").withModuleID("main"),
    ),
    (Compile / fastLinkJS) := {
      (Compile / fastLinkJS)
        .dependsOn(copyCoreLibs)
        .dependsOn(editorRenderer / Compile / fastLinkJS)
        .dependsOn(coreBridge / Compile / fastLinkJS)
        .dependsOn(editorRenderer / copyTreeSitterFiles)
        .value
    },
    javalibEntry := {
      val s = streams.value
      val targetRTJar = target.value / "extracted-rt.jar"
      if (!targetRTJar.exists()) {
        s.log.info(s"Extracting jrt:/modules/java.base/ to $targetRTJar")
        extractRTJar(targetRTJar)
      }
      targetRTJar
    },
    copyCoreLibs := {
      val fullCp0 = Attributed.data((coreBridge / Compile / fullClasspathAsJars).value).toList
        .filter(!_.toString().contains("semanticdb"))
      val fullCp = javalibEntry.value :: fullCp0
      val targetDir = target.value / "libs"
      IO.createDirectory(targetDir)
      val pairs = fullCp.map(f => f -> targetDir / f.getName())
      IO.copy(pairs)
    },
  )
  .dependsOn(editorCommon)

lazy val editorRenderer = project
  .in(file("editor-renderer"))
  .enablePlugins(ScalaJSPlugin, ScalablyTypedConverterExternalNpmPlugin)
  .settings(
    name := "funlaby-editor-renderer",
    scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.ESModule) },
    Compile / scalaJSModuleInitializers +=
      ModuleInitializer.mainMethodWithArgs("com.funlabyrinthe.editor.renderer.Renderer", "main").withModuleID("renderer"),
    libraryDependencies ++= Seq(
      "com.raquo" %%% "laminar" % "16.0.0",
      "be.doeraene" %%% "web-components-ui5" % "1.17.0",
      "com.lihaoyi" %%% "fansi" % "0.4.0",
    ),
    externalNpm := (LocalRootProject / baseDirectory).value,

    copyTreeSitterFiles := {
      import scala.sys.process._

      val s = streams.value

      val targetDir = target.value / "tree-sitter-scala"
      IO.createDirectory(targetDir)

      val webTreeSitter = (LocalRootProject / baseDirectory).value / "node_modules/web-tree-sitter"

      var didAnything = false

      if (!(targetDir / "tree-sitter.js").exists) {
        s.log.info("Copying tree-sitter.js")
        IO.copyFile(webTreeSitter / "tree-sitter.js", targetDir / "tree-sitter.js")
        IO.append(targetDir / "tree-sitter.js", "\nexport default TreeSitter;\n")
        didAnything = true
      }

      if (!(targetDir / "tree-sitter.wasm").exists) {
        s.log.info("Copying tree-sitter.wasm")
        IO.copyFile(webTreeSitter / "tree-sitter.wasm", targetDir / "tree-sitter.wasm")
        didAnything = true
      }

      if (!(targetDir / "tree-sitter-scala.wasm").exists) {
        s.log.info("Downloading tree-sitter-scala.wasm")
        val treeSitterScalaURL =
          url("https://github.com/sjrd/tree-sitter-scala/releases/download/v0.20.2/tree-sitter-scala.wasm")
        (treeSitterScalaURL #> (targetDir / "tree-sitter-scala.wasm")).!
        didAnything = true
      }

      if (!(targetDir / "highlights.scm").exists) {
        s.log.info("Downloading highlights.scm")
        val highlightsURL =
          url("https://raw.githubusercontent.com/tree-sitter/tree-sitter-scala/v0.20.2/queries/scala/highlights.scm")
        (highlightsURL #> (targetDir / "highlights.scm")).!
        didAnything = true
      }

      if (didAnything)
        s.log.info("Done copying tree-sitter files")
    },
  )
  .dependsOn(core, coreInterface, editorCommon)

def extractRTJar(targetRTJar: File): Unit = {
  import java.io.{IOException, FileOutputStream}
  import java.nio.file.{Files, FileSystems}
  import java.util.zip.{ZipEntry, ZipOutputStream}

  import scala.jdk.CollectionConverters._
  import scala.util.control.NonFatal

  val fs = FileSystems.getFileSystem(java.net.URI.create("jrt:/"))

  val zipStream = new ZipOutputStream(new FileOutputStream(targetRTJar))
  try {
    val javaBasePath = fs.getPath("modules", "java.base")
    Files.walk(javaBasePath).forEach({ p =>
      if (Files.isRegularFile(p)) {
        try {
          val data = Files.readAllBytes(p)
          val outPath = javaBasePath.relativize(p).iterator().asScala.mkString("/")
          val ze = new ZipEntry(outPath)
          zipStream.putNextEntry(ze)
          zipStream.write(data)
        } catch {
          case NonFatal(t) =>
            throw new IOException(s"Exception while extracting $p", t)
        }
      }
    })
  } finally {
    zipStream.close()
  }
}
