import sbt._
import Keys._
import Process.cat

import ch.epfl.lamp.sbtscalajs.ScalaJSPlugin._
import ScalaJSKeys._

object FunLabyrintheBuild extends Build {

  val funlabyScalaVersion = "2.10.2"

  val defaultSettings: Seq[Setting[_]] = Defaults.defaultSettings ++ Seq(
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
      libraryDependencies <<= (scalaVersion, libraryDependencies) { (ver, deps) =>
        deps :+ compilerPlugin("org.scala-lang.plugins" % "continuations" % ver)
      },
      scalacOptions += "-P:continuations:enable"
  )

  val javafxSettings: Seq[Setting[_]] = Seq(
      unmanagedJars in Compile += Attributed.blank(
          file(scala.util.Properties.javaHome) / "lib" / "jfxrt.jar"),
      fork in run := true
  )

  val scalafxSettings: Seq[Setting[_]] = javafxSettings ++ Seq(
      libraryDependencies += "org.scalafx" %% "scalafx" % "1.0.0-M4"
  )

  lazy val root = Project(
      id = "funlabyrinthe",
      base = file("."),
      settings = defaultSettings ++ Seq(
          name := "FunLabyrinthe"
      )
  ).aggregate(
      core, mazes, runner, editor
  )

  lazy val core = Project(
      id = "core",
      base = file("core"),
      settings = defaultSettings ++ Seq(
          name := "FunLabyrinthe core"
      )
  )

  lazy val corejs = Project(
      id = "corejs",
      base = file("corejs"),
      settings = defaultSettings ++ scalaJSSettings ++ Seq(
          name := "FunLabyrinthe core js",
          sourceDirectory <<= (sourceDirectory in core)
      )
  )

  lazy val mazes = Project(
      id = "mazes",
      base = file("mazes"),
      settings = defaultSettings ++ Seq(
          name := "FunLabyrinthe mazes"
      )
  ).dependsOn(core)

  lazy val mazesjs = Project(
      id = "mazesjs",
      base = file("mazesjs"),
      settings = defaultSettings ++ scalaJSSettings ++ Seq(
          name := "FunLabyrinthe mazes js",
          sourceDirectory <<= (sourceDirectory in mazes),
          packageJS in Compile <<=
            (packageJS in Compile).dependsOn(packageJS in (corejs, Compile))
      )
  ).dependsOn(corejs)

  lazy val javafxGraphics = Project(
      id = "javafx-graphics",
      base = file("javafx-graphics"),
      settings = defaultSettings ++ javafxSettings ++ Seq(
          name := "JavaFX-based graphics"
      )
  ).dependsOn(core)

  lazy val html5Graphics = Project(
      id = "html5-graphics",
      base = file("html5-graphics"),
      settings = defaultSettings ++ scalaJSSettings ++ Seq(
          name := "HTML5-based graphics",
          packageJS in Compile <<=
            (packageJS in Compile).dependsOn(packageJS in (corejs, Compile))
      )
  ).dependsOn(corejs)

  lazy val runner = Project(
      id = "runner",
      base = file("runner"),
      settings = defaultSettings ++ scalafxSettings ++ Seq(
          name := "FunLabyrinthe runner",
          mainClass := Some("com.funlabyrinthe.runner.Main")
      )
  ).dependsOn(core, mazes, javafxGraphics)

  lazy val runnerjs = Project(
      id = "runnerjs",
      base = file("runnerjs"),
      settings = defaultSettings ++ scalaJSSettings ++ Seq(
          name := "FunLabyrinthe runner js",
          packageJS in Compile <<=
            (packageJS in Compile).dependsOn(
                packageJS in (mazesjs, Compile),
                packageJS in (html5Graphics, Compile)),

          unmanagedSources in (Compile, optimizeJS) <++= (
              baseDirectory
          ) map { base =>
            Seq(base / "js" / "scalajs-runtime.js", base / "js" / "startup.js")
          },

          managedSources in (Compile, optimizeJS) <<= (
              packageJS in (corejs, Compile), packageJS in (mazesjs, Compile),
              packageJS in (html5Graphics, Compile), packageJS in Compile
          ) map { (corePack, mazesPack, graphicsPack, runnerPack) =>
            Seq(corePack, mazesPack, graphicsPack, runnerPack)
          }
      )
  ).dependsOn(corejs, mazesjs, html5Graphics)

  lazy val editor = Project(
      id = "editor",
      base = file("editor"),
      settings = defaultSettings ++ scalafxSettings ++ Seq(
          name := "FunLabyrinthe editor",
          mainClass := Some("com.funlabyrinthe.editor.Main"),
          libraryDependencies ++= Seq(
              "org.scala-lang" % "scala-reflect" % funlabyScalaVersion
          )
      )
  ).dependsOn(core, mazes, javafxGraphics)
}
