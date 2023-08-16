package com.funlabyrinthe.editor.renderer

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import java.io.IOException

import com.funlabyrinthe.core.*
import com.funlabyrinthe.core.pickling.*
import com.funlabyrinthe.core.pickling.flspecific.SpecificPicklers

import com.funlabyrinthe.graphics.html.HTML5GraphicsSystem
import com.funlabyrinthe.htmlenv.ResourceLoader
import com.funlabyrinthe.mazes.{Mazes, Player}

final class UniverseFile private (val projectFile: File, val universe: Universe):
  private val picklingRegistry: PicklingRegistry =
    val registry = new PicklingRegistry(universe)
    SpecificPicklers.registerSpecificPicklers(registry, universe)
    registry
  end picklingRegistry

  private def load(): Future[this.type] =
    for pickleString <- projectFile.readAsString() yield
      val pickle = Pickle.fromString(pickleString)

      /*for moduleClassName <- findAllModules() do
        val cls = classLoader.loadClass(moduleClassName).asSubclass(classOf[Module])
        val ctor = cls.getDeclaredConstructor(classOf[Universe])
        val module = ctor.newInstance(universe)
        universe.addModule(module)*/
      universe.addModule(new Mazes(universe))

      unpickle(pickle)(using createPicklingContext())
      this
  end load

  private def unpickle(pickle: Pickle)(using Context): Unit =
    pickle match
      case pickle: ObjectPickle if pickle.getField("universe").nonEmpty =>
        /*for
          sourcesPickle <- pickle.getField("sources")
          sources <-  Pickleable.unpickle[List[String]](sourcesPickle)
        do
          sourceFiles.clear()
          sourceFiles ++= sources*/

        for universePickle <- pickle.getField("universe") do
          picklingRegistry.unpickle(universe, universePickle)

      case _ =>
        throw IOException(s"The project file does not contain a valid FunLabyrinthe project")
  end unpickle

  def save(): Future[Unit] =
    val pickle = this.pickle()(using createPicklingContext())
    val pickleString = pickle.toString()
    projectFile.writeString(pickleString)

  private def pickle()(using Context): Pickle =
    //val sourcesPickle = Pickleable.pickle(sourceFiles.toList)
    val universePickle = picklingRegistry.pickle(universe)

    ObjectPickle(
      List(
        //"sources" -> sourcesPickle,
        "universe" -> universePickle,
      )
    )
  end pickle

  private def createPicklingContext(): Context =
    new Context {
      val registry: PicklingRegistry = picklingRegistry
    }
end UniverseFile

object UniverseFile:
  def createNew(projectFile: File, globalResourcesDir: File): Future[UniverseFile] =
    val environment = createEnvironment(projectFile, globalResourcesDir)
    val universe = new Universe(environment)
    universe.addModule(new Mazes(universe))
    universe.initialize()
    new Player(using ComponentInit(universe, ComponentID("player"), universe.module[Mazes]))

    Future.successful(new UniverseFile(projectFile, universe))
  end createNew

  def load(projectFile: File, globalResourcesDir: File): Future[UniverseFile] =
    val environment = createEnvironment(projectFile, globalResourcesDir)
    val universe = new Universe(environment)
    new UniverseFile(projectFile, universe).load()
  end load

  private def createEnvironment(projectFile: File, globalResourcesDir: File): UniverseEnvironment =
    val urls = Array(
      projectFile.parent / "Resources",
      globalResourcesDir,
    )

    val resourceLoader = new ResourceLoader("./Resources/")
    new UniverseEnvironment(HTML5GraphicsSystem, resourceLoader)
  end createEnvironment
end UniverseFile
