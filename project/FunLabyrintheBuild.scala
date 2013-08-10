import sbt._
import Keys._
import Process.cat

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

  lazy val mazes = Project(
      id = "mazes",
      base = file("mazes"),
      settings = defaultSettings ++ Seq(
          name := "FunLabyrinthe mazes"
      )
  ).dependsOn(core)

  lazy val javafxGraphics = Project(
      id = "javafx-graphics",
      base = file("javafx-graphics"),
      settings = defaultSettings ++ javafxSettings ++ Seq(
          name := "JavaFX-base graphics"
      )
  ).dependsOn(core)

  lazy val runner = Project(
      id = "runner",
      base = file("runner"),
      settings = defaultSettings ++ scalafxSettings ++ Seq(
          name := "FunLabyrinthe runner",
          mainClass := Some("com.funlabyrinthe.runner.Main")
      )
  ).dependsOn(core, mazes, javafxGraphics)

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
