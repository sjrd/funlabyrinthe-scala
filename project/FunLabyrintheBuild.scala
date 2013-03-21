import sbt._
import Keys._
import Process.cat

object FunLabyrintheBuild extends Build {

  val funlabyScalaVersion = "2.10.1"

  val defaultSettings = Defaults.defaultSettings ++ Seq(
      scalaVersion := funlabyScalaVersion,
      scalacOptions ++= Seq(
          "-deprecation",
          "-unchecked",
          "-feature",
          "-encoding", "utf8"
      ),
      version := "0.1-SNAPSHOT"
  )

  lazy val root = Project(
      id = "funlabyrinthe",
      base = file("."),
      settings = defaultSettings ++ Seq(
          name := "FunLabyrinthe"
      )
  ).aggregate(
      core, runner
  )

  lazy val core = Project(
      id = "core",
      base = file("core"),
      settings = defaultSettings ++ Seq(
          name := "FunLabyrinthe core"
      )
  )

  lazy val runner = Project(
      id = "runner",
      base = file("runner"),
      settings = defaultSettings ++ Seq(
          name := "FunLabyrinthe runner",
          mainClass := Some("com.funlabyrinthe.runner.Main")
      )
  ).dependsOn(core)
}
