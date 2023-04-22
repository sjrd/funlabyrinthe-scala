package com.funlabyrinthe.editor

import com.funlabyrinthe.core.*
import com.funlabyrinthe.mazes.*

import com.funlabyrinthe.graphics.{ jfx => gjfx }
import com.funlabyrinthe.jvmenv.ResourceLoader

import java.net.URLClassLoader

import scalafx.Includes._
import scalafx.scene.Scene
import scalafx.scene.layout._
import scalafx.scene.control._
import scalafx.stage.Stage

final class ProjectSelectorScene(stage: Stage) extends Scene { thisScene =>
  content = new Button("Create new project") {
    this.onAction = { event =>
      createNewProject()
    }
  }

  def createNewProject(): Unit =
    val urls = Array(
      new java.io.File("C:/Users/Public/Documents/FunLabyrinthe/Projects/Temple de l'eau/Resources/").toURI.toURL,
      new java.io.File("C:/Users/Public/Documents/FunLabyrinthe/Library/Resources/").toURI.toURL,
    )
    val resourceLoader = new ResourceLoader(new URLClassLoader(urls, getClass.getClassLoader))

    val environment = new UniverseEnvironment(
        gjfx.JavaFXGraphicsSystem, resourceLoader)

    class MyUniverse extends Universe(environment) with MazeUniverse

    implicit val universe: MyUniverse = new MyUniverse
    universe.initialize()
    import universe._
    import mazes._

    locally {
      val mainMap = new Map(Dimensions(13, 9, 1), mazes.Grass)
      for (pos <- mainMap.minRef until mainMap.maxRef by (2, 2)) {
        pos() = mazes.Wall
      }

      val player = new Player
      player.position = Some(SquareRef(mainMap, Position(1, 1, 0)))
    }

    val universeEditorScene = new UniverseEditorScene(stage, universe)
    stage.scene = universeEditorScene
  end createNewProject
}
