package com.funlabyrinthe.editor

import com.funlabyrinthe.core.*
import java.io.File
import com.funlabyrinthe.mazes.MazeUniverse
import com.funlabyrinthe.mazes.Player
import com.funlabyrinthe.graphics.jfx.JavaFXGraphicsSystem
import java.net.URLClassLoader
import com.funlabyrinthe.jvmenv.ResourceLoader

final class UniverseFile(val projectFile: File, val universe: Universe):
  def save(): Unit =
    ???
end UniverseFile

object UniverseFile:
  final class ActualMazeUniverse(env: UniverseEnvironment) extends Universe(env) with MazeUniverse

  def createNew(projectFile: File, globalResourcesDir: File): UniverseFile =
    val urls = Array(
      new File(projectFile.getParentFile(), "Resources/").toURI.toURL,
      globalResourcesDir.toURI.toURL,
    )

    val resourceLoader = new ResourceLoader(new URLClassLoader(urls, getClass.getClassLoader))
    val environment = new UniverseEnvironment(JavaFXGraphicsSystem, resourceLoader)

    val universe = new ActualMazeUniverse(environment)
    universe.initialize()
    new Player()(universe, ComponentID("player"))

    new UniverseFile(projectFile, universe)
  end createNew
end UniverseFile
