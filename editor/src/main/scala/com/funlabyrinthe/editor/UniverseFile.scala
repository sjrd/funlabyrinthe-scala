package com.funlabyrinthe.editor

import java.io.File
import java.io.IOException
import java.net.URLClassLoader
import java.nio.file.FileSystems

import scala.collection.mutable

import com.funlabyrinthe.core.*
import com.funlabyrinthe.core.pickling.*

import com.funlabyrinthe.graphics.jfx.JavaFXGraphicsSystem
import com.funlabyrinthe.jvmenv.ResourceLoader

import com.funlabyrinthe.mazes.*

final class UniverseFile(val projectFile: File, val universe: Universe):
  val rootDirectory: File = projectFile.getParentFile()
  val sourcesDirectory: File = new File(rootDirectory, "Sources")
  val targetDirectory: File = new File(rootDirectory, "Target")

  val dependencyClasspath =
    System.getenv("FUNLABY_COMPILE_CLASSPATH").split(";").toList.map(new File(_))
  val fullClasspath =
    System.getenv("FUNLABY_FULL_CLASSPATH").split(";").toList.map(new File(_)) :+ targetDirectory

  val sourceFiles: mutable.ArrayBuffer[String] = mutable.ArrayBuffer.empty

  private val classLoader: URLClassLoader =
    new URLClassLoader("project", fullClasspath.map(_.toURI().toURL()).toArray, getClass().getClassLoader())

  private given Context = Context.make(universe)

  private def findAllModules(): List[String] =
    import tastyquery.Classpaths.*
    import tastyquery.Contexts.*
    import tastyquery.Symbols.*

    val javaBase = FileSystems.getFileSystem(java.net.URI.create("jrt:/")).getPath("modules", "java.base")
    val fullClasspathPaths = fullClasspath.map(_.toPath())
    val cp = tastyquery.jdk.ClasspathLoaders.read(javaBase :: fullClasspathPaths)
    val ctx = tastyquery.Contexts.init(cp)

    given Context = ctx

    val ModuleClass = ctx.findTopLevelClass("com.funlabyrinthe.core.Module")
    val builder = List.newBuilder[String]

    for entry <- cp.entries.iterator.drop(1) do // ignore java.base
      println(entry.packages.toList.map(_.dotSeparatedName))
      for case cls: ClassSymbol <- ctx.findSymbolsByClasspathEntry(entry) do
        if cls.parentClasses.contains(ModuleClass) then
          builder += cls.fullName.toString()

    builder.result()
  end findAllModules

  private def load(): this.type =
    val pickleString = java.nio.file.Files.readString(projectFile.toPath())
    val pickle = Pickle.fromString(pickleString)

    for moduleClassName <- findAllModules() do
      val cls = classLoader.loadClass(moduleClassName).asSubclass(classOf[Module])
      val ctor = cls.getDeclaredConstructor(classOf[Universe])
      val module = ctor.newInstance(universe)
      universe.addModule(module)

    unpickle(pickle)
    this
  end load

  private def unpickle(pickle: Pickle)(using Context): Unit =
    pickle match
      case pickle: ObjectPickle if pickle.getField("universe").nonEmpty =>
        for
          sourcesPickle <- pickle.getField("sources")
          sources <-  Pickleable.unpickle[List[String]](sourcesPickle)
        do
          sourceFiles.clear()
          sourceFiles ++= sources

        for universePickle <- pickle.getField("universe") do
          InPlacePickleable.unpickle(universe, universePickle)

      case _ =>
        throw IOException(s"The project file does not contain a valid FunLabyrinthe project")
  end unpickle

  def save(): Unit =
    val pickle = this.pickle()
    val pickleString = pickle.toString()
    java.nio.file.Files.writeString(projectFile.toPath(), pickleString)

  private def pickle()(using Context): Pickle =
    val sourcesPickle = Pickleable.pickle(sourceFiles.toList)
    val universePickle = InPlacePickleable.pickle(universe)

    ObjectPickle(
      List(
        "sources" -> sourcesPickle,
        "universe" -> universePickle,
      )
    )
  end pickle
end UniverseFile

object UniverseFile:
  def createNew(projectFile: File, globalResourcesDir: File): UniverseFile =
    val environment = createEnvironment(projectFile, globalResourcesDir)
    val universe = new Universe(environment)
    universe.addModule(new Mazes(universe))
    universe.initialize()

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
