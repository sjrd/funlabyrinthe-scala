import org.scalajs.linker.interface.ModuleInitializer

val javalibEntry = taskKey[File]("Path to rt.jar or \"jrt:/\"")
val copyCoreLibs = taskKey[Unit]("copy core libs")

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

val javafxSettings = Def.settings(
  // Add dependency on JavaFX libraries, OS dependent
  libraryDependencies ++= {
    // Determine OS version of JavaFX binaries
    val osName = System.getProperty("os.name") match {
      case n if n.startsWith("Linux")   => "linux"
      case n if n.startsWith("Mac")     => "mac"
      case n if n.startsWith("Windows") => "win"
      case n => throw new Exception("Unknown platform: " + n)
    }

    val javaFXModules = Seq("base", "controls", "fxml", "graphics", "media", "swing", "web")

    javaFXModules.map { m =>
      "org.openjfx" % s"javafx-$m" % "19.0.2" classifier osName
    }
  },

  run / fork := true,
)

val scalafxSettings = Def.settings(
  javafxSettings,
  libraryDependencies += "org.scalafx" %% "scalafx" % "19.0.0-R30",
)

val testSettings = Def.settings(
  libraryDependencies += "org.scalameta" %%% "munit" % "0.7.29" % Test,
)

lazy val root = project.in(file("."))
  .settings(
    name := "FunLabyrinthe",
  )
  .aggregate(
    core.jvm,
    core.js,
    mazes.jvm,
    mazes.js,
    javafxGraphics,
    html5Graphics,
    runner.jvm,
    runner.js,
    editor,
  )

lazy val core = crossProject(JVMPlatform, JSPlatform)
  .crossType(CrossType.Pure)
  .settings(
    name := "funlaby-core",
    libraryDependencies += "com.github.rssh" %%% "dotty-cps-async" % "0.9.17",
    testSettings,
  )
  .jsSettings(
    Compile / unmanagedSourceDirectories += baseDirectory.value.getParentFile / "js/src/main/scala",
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
  .dependsOn(coreInterface, core.js, mazes.js, html5Graphics)

lazy val mazes = crossProject(JVMPlatform, JSPlatform)
  .crossType(CrossType.Pure)
  .settings(
    name := "funlaby-mazes",
  )
  .dependsOn(core)

lazy val javafxGraphics = project.in(file("javafx-graphics"))
  .settings(
    javafxSettings,
    name := "funlaby-graphics-javafx",
  )
  .dependsOn(core.jvm)

lazy val html5Graphics = project.in(file("html5-graphics"))
  .enablePlugins(ScalaJSPlugin)
  .settings(
    name := "funlaby-graphics-dom",
    libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "2.4.0",
  )
  .dependsOn(core.js)

lazy val runner = crossProject(JVMPlatform, JSPlatform)
  .settings(
    name := "funlaby-runner",
  )
  .jvmSettings(
    scalafxSettings,
  )
  .jsSettings(
    scalaJSUseMainModuleInitializer := true,
    libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "2.4.0",
  )
  .dependsOn(core, mazes)
  .jvmConfigure(_.dependsOn(javafxGraphics))
  .jsConfigure(_.dependsOn(html5Graphics))

lazy val editor = project
  .settings(
    name := "funlaby-editor",
    scalafxSettings,
    libraryDependencies ++= Seq(
      "org.fxmisc.richtext" % "richtextfx" % "0.11.0",
      "ch.epfl.scala" %%% "tasty-query" % "0.9.2",
    ),
    envVars ++= {
      val fullCp = Attributed.data((mazes.jvm / Compile / fullClasspath).value).toList
        .filter(!_.toString().contains("semanticdb"))
      val compileCp = fullCp.toList.filter { file =>
        !file.toString().replace('\\', '/').contains("/org/scala-lang/")
      }
      Map(
        "FUNLABY_FULL_CLASSPATH" -> fullCp.mkString(";"),
        "FUNLABY_COMPILE_CLASSPATH" -> compileCp.mkString(";"),
      )
    },
  )
  .dependsOn(core.jvm, mazes.jvm, javafxGraphics)

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
        .dependsOn(editorRenderer / Compile / fastLinkJS)
        .dependsOn(coreBridge / Compile / fastLinkJS)
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
      val fullCp0 = Attributed.data((mazes.jvm / Compile / fullClasspathAsJars).value).toList
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
      "be.doeraene" %%% "web-components-ui5" % "1.10.0",
    ),
    externalNpm := (LocalRootProject / baseDirectory).value,
  )
  .dependsOn(core.js, html5Graphics, coreInterface, editorCommon)

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
