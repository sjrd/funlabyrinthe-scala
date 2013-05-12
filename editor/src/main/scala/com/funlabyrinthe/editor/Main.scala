package com.funlabyrinthe.editor

import com.funlabyrinthe._
import core._
import core.graphics._
import mazes._

import java.net._

import scalafx.Includes._
import scalafx.application.JFXApp
import scalafx.scene.Scene
import scalafx.scene.layout._
import scalafx.scene.control._

object Main extends JFXApp {
  private val universe: Universe = {
    new Universe with MazeUniverse {
      override lazy val classLoader = new URLClassLoader(
          Array(
              new java.io.File("C:/Users/Public/Documents/FunLabyrinthe/Projects/Temple de l'eau/Resources/").toURI.toURL,
              new java.io.File("C:/Users/Public/Documents/FunLabyrinthe/Library/Resources/").toURI.toURL),
          getClass.getClassLoader)
    }
  }
  universe.initialize()

  stage = new JFXApp.PrimaryStage { stage0 =>
    title = "FunLabyrinthe editor"
    width = 600
    height = 500
    scene = new Scene { scene0 =>
      content = new UniverseEditor(stage0)(universe) {
        prefWidth <== scene0.width
        prefHeight <== scene0.height
      }
      stylesheets += Main.getClass.getResource("editor.css").toExternalForm()
      stylesheets += classOf[inspector.jfx.Inspector].getResource("inspector.css").toExternalForm()
    }
  }
}
