package com.funlabyrinthe.editor

import java.io.File
import java.net.URLClassLoader

import com.funlabyrinthe.core.*
import com.funlabyrinthe.core.pickling.*
import com.funlabyrinthe.core.pickling.flspecific.SpecificPicklers

import com.funlabyrinthe.graphics.jfx.JavaFXGraphicsSystem
import com.funlabyrinthe.jvmenv.ResourceLoader

import com.funlabyrinthe.mazes.*

final class UniverseFile(val projectFile: File, val universe: Universe):
  private val picklingRegistry: PicklingRegistry =
    val registry = new PicklingRegistry
    SpecificPicklers.registerSpecificPicklers(registry, universe)
    registry.registerModule(new Mazes(_))
    registry
  end picklingRegistry

  private def load(): this.type =
    val pickleString = java.nio.file.Files.readString(projectFile.toPath())
    val pickle = Pickle.fromString(pickleString)
    picklingRegistry.unpickle(universe, pickle)
    this
  end load

  def save(): Unit =
    val pickle = picklingRegistry.pickle(universe)
    val pickleString = pickle.toString()
    java.nio.file.Files.writeString(projectFile.toPath(), pickleString)
end UniverseFile

object UniverseFile:
  def createNew(projectFile: File, globalResourcesDir: File): UniverseFile =
    val environment = createEnvironment(projectFile, globalResourcesDir)
    val universe = new Universe(environment)
    universe.addModule(new Mazes(universe))
    universe.initialize()
    new Player()(universe, ComponentID("player"))

    new UniverseFile(projectFile, universe)
  end createNew

  def load(projectFile: File, globalResourcesDir: File): UniverseFile =
    val environment = createEnvironment(projectFile, globalResourcesDir)
    val universe = new Universe(environment)
    new UniverseFile(projectFile, universe).load()
  end load

  private def createEnvironment(projectFile: File, globalResourcesDir: File): UniverseEnvironment =
    val urls = Array(
      new File(projectFile.getParentFile(), "Resources/").toURI.toURL,
      globalResourcesDir.toURI.toURL,
    )

    val resourceLoader = new ResourceLoader(new URLClassLoader(urls, getClass.getClassLoader))
    new UniverseEnvironment(JavaFXGraphicsSystem, resourceLoader)
  end createEnvironment
end UniverseFile
