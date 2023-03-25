import sbt._
import Keys._

import scala.scalajs.sbtplugin.ScalaJSPlugin._
import ScalaJSKeys._

object FunLabyrintheBuild extends Build {

  val funlabyScalaVersion = "2.10.2"

  val defaultSettings: Seq[Setting[_]] = Seq(
      scalaVersion := funlabyScalaVersion,
      scalacOptions ++= Seq(
          "-deprecation",
          "-unchecked",
          "-feature",
          "-encoding", "utf8"
      ),
      version := "0.1-SNAPSHOT",

      // Continuation plugin
      autoCompilerPlugins := true,
      libraryDependencies += compilerPlugin(
          "org.scala-lang.plugins" % "continuations" % scalaVersion.value),
      scalacOptions += "-P:continuations:enable"
  )

  val javafxSettings: Seq[Setting[_]] = Seq(
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
          "org.openjfx" % s"javafx-$m" % "14.0.1" classifier osName
        }
      },

      fork in run := true
  )

  val scalafxSettings: Seq[Setting[_]] = javafxSettings ++ Seq(
      libraryDependencies += "org.scalafx" %% "scalafx" % "14-R19"
  )

  lazy val root = project.in(file(".")).settings(
      defaultSettings: _*
  ).settings(
      name := "FunLabyrinthe"
  ).aggregate(
      core, mazes, runner, editor
  )

  lazy val coremacros = project.settings(
      name := "FunLabyrinthe core macros",
      libraryDependencies ++= Seq(
          "org.scala-lang" % "scala-reflect" % scalaVersion.value,
          "org.scala-lang" % "scala-compiler" % scalaVersion.value
      ),
      scalacOptions ++= Seq(
          "-sourcepath",
          (baseDirectory.value / ".." / "core" / "src" / "main" / "scala").getAbsolutePath)
  )

  lazy val core = project.settings(
      defaultSettings: _*
  ).settings(
      name := "FunLabyrinthe core"
  ).dependsOn(coremacros)

  lazy val corejs = project.settings(
      (defaultSettings ++ scalaJSSettings): _*
  ).settings(
      name := "FunLabyrinthe core js",
      sourceDirectory := (sourceDirectory in core).value
  ).dependsOn(coremacros)

  lazy val mazes = project.settings(
      defaultSettings: _*
  ).settings(
      name := "FunLabyrinthe mazes"
  ).dependsOn(core)

  lazy val mazesjs = project.settings(
      (defaultSettings ++ scalaJSSettings): _*
  ).settings(
      name := "FunLabyrinthe mazes js",
      sourceDirectory := (sourceDirectory in mazes).value
  ).dependsOn(corejs)

  lazy val javafxGraphics = project.in(file("javafx-graphics")).settings(
      (defaultSettings ++ javafxSettings): _*
  ).settings(
      name := "JavaFX-based graphics"
  ).dependsOn(core)

  lazy val html5Graphics = project.in(file("html5-graphics")).settings(
      (defaultSettings ++ scalaJSSettings): _*
  ).settings(
      name := "HTML5-based graphics",
      libraryDependencies += "org.scala-lang.modules.scalajs" %% "scalajs-dom" % "0.3"
  ).dependsOn(corejs)

  lazy val runner = project.settings(
      (defaultSettings ++ scalafxSettings): _*
  ).settings(
      name := "FunLabyrinthe runner",
      mainClass := Some("com.funlabyrinthe.runner.Main")
  ).dependsOn(core, mazes, javafxGraphics)

  lazy val runnerjs = project.settings(
      (defaultSettings ++ scalaJSSettings): _*
  ).settings(
      name := "FunLabyrinthe runner js",
      libraryDependencies += "org.scala-lang.modules.scalajs" %% "scalajs-dom" % "0.3"
  ).dependsOn(corejs, mazesjs, html5Graphics)

  lazy val editor = project.settings(
      (defaultSettings ++ scalafxSettings): _*
  ).settings(
      name := "FunLabyrinthe editor",
      mainClass := Some("com.funlabyrinthe.editor.Main"),
      libraryDependencies ++= Seq(
          "org.scala-lang" % "scala-reflect" % funlabyScalaVersion
      )
  ).dependsOn(core, mazes, javafxGraphics)
}
