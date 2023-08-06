import java.nio.file.FileSystems
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
