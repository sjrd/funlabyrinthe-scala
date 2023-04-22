package com.funlabyrinthe.editor

import java.io.File
import java.net.URLClassLoader

import com.funlabyrinthe.core.*
import com.funlabyrinthe.core.pickling.*
import com.funlabyrinthe.mazes.MazeUniverse
import com.funlabyrinthe.mazes.Player
import com.funlabyrinthe.graphics.jfx.JavaFXGraphicsSystem
import com.funlabyrinthe.jvmenv.ResourceLoader
import com.funlabyrinthe.core.pickling.flspecific.SpecificPicklers

final class UniverseFile(val projectFile: File, val universe: Universe):
  private val picklingRegistry: PicklingRegistry =
    val registry = new PicklingRegistry
    SpecificPicklers.registerSpecificPicklers(registry, universe)
    registry
  end picklingRegistry

  def save(): Unit =
    val pickle = picklingRegistry.pickle(universe)
    val pickleString = pickle.toString()
    java.nio.file.Files.writeString(projectFile.toPath(), pickleString)
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
