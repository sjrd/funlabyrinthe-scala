package com.funlabyrinthe.editor

import com.funlabyrinthe.core.*
import com.funlabyrinthe.mazes.*
import com.funlabyrinthe.mazes.MazeUniverse.*

import java.io.File
import java.net.URLClassLoader

import scalafx.Includes._
import scalafx.scene.Scene
import scalafx.scene.layout._
import scalafx.scene.control._
import scalafx.stage.FileChooser
import scalafx.stage.Stage

final class ProjectSelectorScene(stage: Stage) extends Scene { thisScene =>
  val globalResourcesDir = new File("C:/Users/Public/Documents/FunLabyrinthe/Library/Resources/")

  content = new Button("Create new project") {
    this.onAction = { event =>
      createNewProject()
    }
  }

  def createNewProject(): Unit =
    val fileChooser = new FileChooser {
      title = "Create a new new project"
      extensionFilters ++= Seq(
        new FileChooser.ExtensionFilter("FunLabyrinthe project", ".funlaby"),
      )
    }
    val selectedFile = fileChooser.showSaveDialog(stage)
    if selectedFile != null then
      doCreateNewProject(selectedFile)
  end createNewProject

  def doCreateNewProject(projectFile: File): Unit =
    val universeFile = UniverseFile.createNew(projectFile, globalResourcesDir)
    val universe = universeFile.universe

    locally {
      given MazeUniverse = universe.asMazeUniverse

      val mazes = universe.mazes

      val mainMap = new Map(Dimensions(13, 9, 1), mazes.Grass)
      for (pos <- mainMap.minRef until mainMap.maxRef by (2, 2)) {
        pos() = mazes.Wall
      }

      val player = universe.getComponentByID("player").asInstanceOf[Player]
      player.position = Some(SquareRef(mainMap, Position(1, 1, 0)))
    }

    val universeEditorScene = new UniverseEditorScene(stage, universeFile)
    stage.scene = universeEditorScene
  end doCreateNewProject
}
