package com.funlabyrinthe.editor

import com.funlabyrinthe.core.*
import com.funlabyrinthe.mazes.*
import com.funlabyrinthe.mazes.Mazes.mazes

import java.io.File
import java.net.URLClassLoader

import scalafx.Includes._
import scalafx.scene.Scene
import scalafx.scene.layout._
import scalafx.scene.control._
import scalafx.stage.FileChooser
import scalafx.stage.Stage

import scalafx.geometry.Insets

final class ProjectSelectorScene(stage: Stage) extends Scene { thisScene =>
  val globalResourcesDir = new File("C:/Users/Public/Documents/FunLabyrinthe/Library/Resources/")

  content = {
    new VBox {
      vgrow = Priority.Always
      hgrow = Priority.Always
      spacing = 10
      margin = Insets(50, 0, 0, 50)
      children = List(
        new Button("Create new project") {
          this.onAction = { event =>
            createNewProject()
          }
        },
        new Button("Load a project") {
          this.onAction = { event =>
            loadProject()
          }
        },
      )
    }
  }

  def createNewProject(): Unit =
    val fileChooser = new FileChooser {
      title = "Create a new project"
      extensionFilters ++= Seq(
        new FileChooser.ExtensionFilter("FunLabyrinthe project", "*.funlaby"),
      )
    }
    val selectedFile = fileChooser.showSaveDialog(stage)
    if selectedFile != null then
      doCreateNewProject(selectedFile)
  end createNewProject

  def loadProject(): Unit =
    val fileChooser = new FileChooser {
      title = "Load a project"
      extensionFilters ++= Seq(
        new FileChooser.ExtensionFilter("FunLabyrinthe project", "*.funlaby"),
      )
    }
    val selectedFile = fileChooser.showOpenDialog(stage)
    if selectedFile != null then
      doLoadProject(selectedFile)
  end loadProject

  def doCreateNewProject(projectFile: File): Unit =
    val universeFile = UniverseFile.createNew(projectFile, globalResourcesDir)
    val universe = universeFile.universe

    locally {
      given Universe = universe

      val mainMap = mazes.MapCreator.createNewComponent()
      mainMap.resize(Dimensions(13, 9, 1), mazes.Grass)
      for (pos <- mainMap.minRef until mainMap.maxRef by (2, 2)) {
        pos() = mazes.Wall
      }

      val player = universe.getComponentByID("player").asInstanceOf[Player]
      player.position = Some(SquareRef(mainMap, Position(1, 1, 0)))
    }

    switchToUniverseEditorScene(universeFile)
  end doCreateNewProject

  def doLoadProject(projectFile: File): Unit =
    val universeFile = UniverseFile.load(projectFile, globalResourcesDir)
    switchToUniverseEditorScene(universeFile)
  end doLoadProject

  private def switchToUniverseEditorScene(universeFile: UniverseFile): Unit =
    val universeEditorScene = new UniverseEditorScene(stage, universeFile)
    stage.scene = universeEditorScene
  end switchToUniverseEditorScene
}
