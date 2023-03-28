val SourceDeps = config("sourcedeps")

inThisBuild(Def.settings(
  scalaVersion := "3.2.2",
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
  )

lazy val core = project
  .settings(
    name := "FunLabyrinthe core",
    libraryDependencies += "com.github.rssh" %%% "dotty-cps-async" % "0.9.16",
  )
  .dependsOn(coremacros)

lazy val corejs = project
  .enablePlugins(ScalaJSPlugin)
  .settings(
    name := "FunLabyrinthe core js",
    sourceDirectory := (core / sourceDirectory).value,
    libraryDependencies += "com.github.rssh" %%% "dotty-cps-async" % "0.9.16",
  )
  .dependsOn(coremacros)

lazy val mazes = project
  .settings(
    name := "FunLabyrinthe mazes",
  )
  .dependsOn(core)

lazy val mazesjs = project
  .enablePlugins(ScalaJSPlugin)
  .settings(
    name := "FunLabyrinthe mazes js",
    sourceDirectory := (mazes / sourceDirectory).value,
  )
  .dependsOn(corejs)

lazy val javafxGraphics = project.in(file("javafx-graphics"))
  .settings(
    javafxSettings,
    name := "JavaFX-based graphics",
  )
  .dependsOn(core)

lazy val html5Graphics = project.in(file("html5-graphics"))
  .enablePlugins(ScalaJSPlugin)
  .settings(
    name := "HTML5-based graphics",
    libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "2.4.0",
  )
  .dependsOn(corejs)

lazy val runner = project
  .settings(
    scalafxSettings,
    name := "FunLabyrinthe runner",
  )
  .dependsOn(core, mazes, javafxGraphics)

lazy val runnerjs = project
  .enablePlugins(ScalaJSPlugin)
  .settings(
    name := "FunLabyrinthe runner js",
    scalaJSUseMainModuleInitializer := true,
    libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "2.4.0",
  )
  .dependsOn(corejs, mazesjs, html5Graphics)

lazy val editor = project
  .settings(
    scalafxSettings,
    name := "FunLabyrinthe editor",
    // to be able to compile, but obviously it does not run
    libraryDependencies += "org.scala-lang" % "scala-reflect" % "2.13.10",
  )
  .dependsOn(core, mazes, javafxGraphics)
