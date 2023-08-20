package com.funlabyrinthe.editor.renderer

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import scala.scalajs.js

import java.io.IOException

import com.funlabyrinthe.core.pickling.*
import com.funlabyrinthe.core.pickling.flspecific.SpecificPicklers

import com.funlabyrinthe.coreinterface.{FunLabyInterface, Universe}
import scala.concurrent.Promise

final class UniverseFile private (val projectFile: File, val intf: FunLabyInterface):
  private var _universe: Option[Universe] = None

  def universe: Universe =
    _universe.getOrElse {
      throw IllegalStateException(s"The universe is not ready yet")
    }

  private def createNew(): Future[this.type] =
    for universe <- intf.createNewUniverse().toFuture yield
      _universe = Some(universe)
      this
  end createNew

  private def load(): Future[this.type] =
    projectFile.readAsString().flatMap { pickleString =>
      val pickle = Pickle.fromString(pickleString)
      unpickle(pickle)
    }
  end load

  private def unpickle(pickle: Pickle): Future[this.type] =
    pickle match
      case pickle: ObjectPickle if pickle.getField("universe").nonEmpty =>
        /*for
          sourcesPickle <- pickle.getField("sources")
          sources <-  Pickleable.unpickle[List[String]](sourcesPickle)
        do
          sourceFiles.clear()
          sourceFiles ++= sources*/

        val universePickle = pickle.getField("universe").get
        val jsonPickle = JSONPickle.pickleToJSON(universePickle).asInstanceOf[js.Object]
        for universe <- intf.loadUniverse(jsonPickle).toFuture yield
          _universe = Some(universe)
          this

      case _ =>
        throw IOException(s"The project file does not contain a valid FunLabyrinthe project")
  end unpickle

  def save(): Future[Unit] =
    val pickle = this.pickle()
    val pickleString = pickle.toString()
    projectFile.writeString(pickleString)
  end save

  private def pickle(): Pickle =
    //val sourcesPickle = Pickleable.pickle(sourceFiles.toList)
    val universeJSONPickle = universe.save()
    val universePickle = JSONPickle.jsonToPickle(universeJSONPickle)

    ObjectPickle(
      List(
        //"sources" -> sourcesPickle,
        "universe" -> universePickle,
      )
    )
  end pickle
end UniverseFile

object UniverseFile:
  private val coreBridgeModulePath =
    "./../../../../core-bridge/target/scala-3.3.0/funlaby-core-bridge-fastopt/main.js"

  def createNew(projectFile: File, globalResourcesDir: File): Future[UniverseFile] =
    for
      intf <- loadFunLabyInterface(coreBridgeModulePath)
      universeFile <- new UniverseFile(projectFile, intf).createNew()
    yield
      universeFile
  end createNew

  def load(projectFile: File, globalResourcesDir: File): Future[UniverseFile] =
    for
      intf <- loadFunLabyInterface(coreBridgeModulePath)
      universeFile <- new UniverseFile(projectFile, intf).load()
    yield
      universeFile
  end load

  private def loadFunLabyInterface(modulePath: String): Future[FunLabyInterface] =
    js.`import`[FunLabyInterfaceModule](modulePath).`then`(_.FunLabyInterface).toFuture

  private trait FunLabyInterfaceModule extends js.Any:
    val FunLabyInterface: FunLabyInterface
  end FunLabyInterfaceModule
end UniverseFile
