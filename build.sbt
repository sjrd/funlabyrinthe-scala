val SourceDeps = config("sourcedeps")

inThisBuild(Def.settings(
  scalaVersion := "2.12.17",
  scalacOptions ++= Seq(
      "-deprecation",
      "-unchecked",
      "-feature",
      "-encoding", "utf8",
  ),
  version := "0.1-SNAPSHOT",
))

val defaultSettings = Def.settings(
  // Continuations plugin
  autoCompilerPlugins := true,
  addCompilerPlugin("org.scala-lang.plugins" % "scala-continuations-plugin_2.12.2" % "1.0.3"),
  scalacOptions += "-P:continuations:enable",
)

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

lazy val root = project.in(file("."))
  .settings(
    name := "FunLabyrinthe",
  )
  .aggregate(
    core, mazes, runner, editor
  )

lazy val coremacros = project
  .settings(
    name := "FunLabyrinthe core macros",
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-reflect" % scalaVersion.value,
      "org.scala-lang" % "scala-compiler" % scalaVersion.value,
    )
  )

// Recompiles scala-continuations-library from sources for Scala.js
lazy val continuationsLibJS = project.in(file("continuations-library-js"))
  .enablePlugins(ScalaJSPlugin)
  .settings(
    defaultSettings,
    name := "continuations-lib-js",

    ivyConfigurations += SourceDeps.hide,
    transitiveClassifiers := Seq("sources"),
    libraryDependencies +=
      ("org.scala-lang.plugins" %% "scala-continuations-library" % "1.0.3" % "sourcedeps"),

    (Compile / sourceGenerators) += Def.task {
      val s = streams.value
      val cacheDir = s.cacheDirectory
      val trgDir = (Compile / sourceManaged).value / "continuations-lib-src"

      val report = updateClassifiers.value
      val sourcesJar = report.select(
          configuration = configurationFilter("sourcedeps"),
          module = (_: ModuleID).name.startsWith("scala-continuations-library_"),
          artifact = artifactFilter(`type` = "src")).headOption.getOrElse {
        sys.error(s"Could not fetch scala-continuations-library sources")
      }

      FileFunction.cached(cacheDir / s"fetchContinuationsLibSource",
          FilesInfo.lastModified, FilesInfo.exists) { dependencies =>
        s.log.info(s"Unpacking scala-continuations-library sources to $trgDir...")
        if (trgDir.exists)
          IO.delete(trgDir)
        IO.createDirectory(trgDir)
        IO.unzip(sourcesJar, trgDir)

        val libSources = (trgDir ** "*.scala").get.toSet
        libSources.foreach(f => {
          val lines = IO.readLines(f)
          IO.writeLines(f, lines)
        })
        libSources
      } (Set(sourcesJar)).toSeq
    }.taskValue,
  )

lazy val core = project
  .settings(
    defaultSettings,
    name := "FunLabyrinthe core",
    libraryDependencies += "org.scala-lang.plugins" %% "scala-continuations-library" % "1.0.3",
  )
  .dependsOn(coremacros)

lazy val corejs = project
  .enablePlugins(ScalaJSPlugin)
  .settings(
    defaultSettings,
    name := "FunLabyrinthe core js",
    sourceDirectory := (core / sourceDirectory).value,
  )
  .dependsOn(continuationsLibJS, coremacros)

lazy val mazes = project
  .settings(
    defaultSettings,
    name := "FunLabyrinthe mazes",
  )
  .dependsOn(core)

lazy val mazesjs = project
  .enablePlugins(ScalaJSPlugin)
  .settings(
    defaultSettings,
    name := "FunLabyrinthe mazes js",
    sourceDirectory := (mazes / sourceDirectory).value,
  )
  .dependsOn(corejs)

lazy val javafxGraphics = project.in(file("javafx-graphics"))
  .settings(
    defaultSettings,
    javafxSettings,
    name := "JavaFX-based graphics",
  )
  .dependsOn(core)

lazy val html5Graphics = project.in(file("html5-graphics"))
  .enablePlugins(ScalaJSPlugin)
  .settings(
    defaultSettings,
    name := "HTML5-based graphics",
    libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "2.4.0",
  )
  .dependsOn(corejs)

lazy val runner = project
  .settings(
    defaultSettings,
    scalafxSettings,
    name := "FunLabyrinthe runner",
  )
  .dependsOn(core, mazes, javafxGraphics)

lazy val runnerjs = project
  .enablePlugins(ScalaJSPlugin)
  .settings(
    defaultSettings,
    name := "FunLabyrinthe runner js",
    scalaJSUseMainModuleInitializer := true,
    libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "2.4.0",
  )
  .dependsOn(corejs, mazesjs, html5Graphics)

lazy val editor = project
  .settings(
    defaultSettings,
    scalafxSettings,
    name := "FunLabyrinthe editor",
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-reflect" % scalaVersion.value,
    ),
  )
  .dependsOn(core, mazes, javafxGraphics)
