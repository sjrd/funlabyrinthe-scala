package com.funlabyrinthe.editor.renderer

import scala.collection.mutable
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import scala.scalajs.js

import java.io.IOException

import com.funlabyrinthe.core.pickling.*
import com.funlabyrinthe.core.pickling.flspecific.SpecificPicklers

import com.funlabyrinthe.coreinterface.{FunLabyInterface, Universe}
import com.funlabyrinthe.editor.renderer.electron.fileService

final class UniverseFile private (
  val projectFile: File,
  coreLibs: js.Array[String],
  val intf: FunLabyInterface,
):
  import UniverseFile.*

  private var _universe: Option[Universe] = None

  val rootDirectory: File = projectFile.parent
  val universeFile: File = rootDirectory / "universe.json"
  val sourcesDirectory: File = rootDirectory / "Sources"
  val targetDirectory: File = rootDirectory / "Target"

  val dependencyClasspath = coreLibs.filter(!ScalaLibraryName.matches(_)).map(new File(_))
  val fullClasspath = coreLibs.map(new File(_)) :+ targetDirectory

  val sourceFiles: mutable.ArrayBuffer[String] = mutable.ArrayBuffer.empty

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
    val f = for
      pickleString <- projectFile.readAsString()
      universePickleString <- universeFile.readAsString()
    yield
      val pickle = Pickle.fromString(pickleString)
      unpickle(pickle)

      for universe <- intf.loadUniverse(universePickleString).toFuture yield
        _universe = Some(universe)
    end f

    f.flatten.map(_ => this)
  end load

  private def unpickle(pickle: Pickle): Unit =
    pickle match
      case pickle: ObjectPickle /*if pickle.getField("universe").nonEmpty*/ =>
        for
          case sourcesPickle: ListPickle <- pickle.getField("sources")
        do
          sourceFiles.clear()
          sourceFiles ++= sourcesPickle.elems.map(_.asInstanceOf[StringPickle].value)

      case _ =>
        throw IOException(s"The project file does not contain a valid FunLabyrinthe project")
  end unpickle

  def save(): Future[Unit] =
    val pickle = this.pickle()
    val pickleString = pickle.toString()

    val universePickleString = universe.save()

    projectFile.writeString(pickleString)
      .flatMap(_ => universeFile.writeString(universePickleString))
  end save

  private def pickle(): Pickle =
    val sourcesPickle = ListPickle(sourceFiles.toList.map(StringPickle(_)))

    ObjectPickle(
      List(
        "sources" -> sourcesPickle,
      )
    )
  end pickle
end UniverseFile

object UniverseFile:
  private val ScalaLibraryName = raw"""/(?:scala-library|scala3-library_3)-[.0-9]+\.jar$$""".r
  private val coreBridgeModulePath =
    "./../../../../core-bridge/target/scala-3.3.0/funlaby-core-bridge-fastopt/main.js"

  def createNew(projectFile: File, globalResourcesDir: File): Future[UniverseFile] =
    for
      coreLibs <- fileService.funlabyCoreLibs().toFuture
      intf <- loadFunLabyInterface(projectFile)
      universeFile <- new UniverseFile(projectFile, coreLibs, intf).createNew()
    yield
      universeFile
  end createNew

  def load(projectFile: File, globalResourcesDir: File): Future[UniverseFile] =
    for
      coreLibs <- fileService.funlabyCoreLibs().toFuture
      intf <- loadFunLabyInterface(projectFile)
      universeFile <- new UniverseFile(projectFile, coreLibs, intf).load()
    yield
      universeFile
  end load

  private def loadFunLabyInterface(projectFile: File): Future[FunLabyInterface] =
    val runtimeUnderTestFile = projectFile.parent / "runtime-under-test.js"

    def load(modulePath: String): Future[FunLabyInterfaceModule] =
      js.`import`[FunLabyInterfaceModule](modulePath).toFuture

    val loadedModule =
      load(runtimeUnderTestFile.path).recoverWith {
        case js.JavaScriptException(e) =>
          println(s"could not load $runtimeUnderTestFile; falling back on default")
          load(coreBridgeModulePath)
      }

    loadedModule.map(_.FunLabyInterface)
  end loadFunLabyInterface

  private trait FunLabyInterfaceModule extends js.Any:
    val FunLabyInterface: FunLabyInterface
  end FunLabyInterfaceModule
end UniverseFile
