package com.funlabyrinthe.editor.renderer

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import com.raquo.laminar.api.L.{*, given}

import be.doeraene.webcomponents.ui5

import com.funlabyrinthe.core.*
import com.funlabyrinthe.htmlenv.ResourceLoader
import com.funlabyrinthe.graphics.html as ghtml
import com.funlabyrinthe.mazes.*

import com.funlabyrinthe.editor.renderer.electron.fileService

class ProjectSelector(selectProjectWriter: Observer[Option[UniverseFile]])(using ErrorHandler):
  private val globalResourcesDir = File("./Resources")

  lazy val topElement: Element =
    div(
      ui5.Button(
        "New project",
        _.events.onClick --> (event => ErrorHandler.handleErrors(createNewProject())),
      ),
      ui5.Button(
        "Load project",
        _.events.onClick --> (event => ErrorHandler.handleErrors(loadProject())),
      )
    )
  end topElement

  private def createNewProject(): Future[Unit] =
    for
      projectFile <- selectNewProjectFile()
      universeFile <- UniverseFile.createNew(projectFile, globalResourcesDir)
    yield
      locally {
        import com.funlabyrinthe.mazes.Mazes.mazes

        val universe = universeFile.universe
        given Universe = universe

        val mainMap = mazes.MapCreator.createNewComponent()
        mainMap.resize(Dimensions(13, 9, 1), mazes.Grass)
        for (pos <- mainMap.minRef until mainMap.maxRef by (2, 2)) {
          pos() = mazes.Wall
        }

        val player = universe.getComponentByID("player").asInstanceOf[Player]
        player.position = Some(SquareRef(mainMap, Position(1, 1, 0)))
      }

      selectProjectWriter.onNext(Some(universeFile))
    end for
  end createNewProject

  private def loadProject(): Future[Unit] =
    for
      projectFile <- selectExistingProjectFile()
      universeFile <- UniverseFile.load(projectFile, globalResourcesDir)
    yield
      selectProjectWriter.onNext(Some(universeFile))
  end loadProject

  private def selectNewProjectFile(): Future[File] =
    for result <- fileService.showSaveNewProjectDialog().toFuture yield
      result.map(new File(_)).getOrElse(UserCancelException.cancel())
  end selectNewProjectFile

  private def selectExistingProjectFile(): Future[File] =
    for result <- fileService.showOpenProjectDialog().toFuture yield
      result.map(new File(_)).getOrElse(UserCancelException.cancel())
  end selectExistingProjectFile
end ProjectSelector
