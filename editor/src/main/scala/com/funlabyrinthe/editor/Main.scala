package com.funlabyrinthe.editor

import com.funlabyrinthe.core._
import com.funlabyrinthe.core.graphics._
import com.funlabyrinthe.mazes._

import java.net._

import scalafx.Includes._
import scalafx.application.JFXApp
import scalafx.scene.Scene
import scalafx.scene.shape.Rectangle
import scalafx.stage.Stage

import scalafx.geometry.Insets

import scalafx.scene.layout._
import scalafx.scene.control._
import scalafx.scene.image.{ Image, ImageView }

import scalafx.util.Duration
import scalafx.geometry.{ Pos, Rectangle2D }

object Main extends JFXApp {
  stage = new JFXApp.PrimaryStage {
    title = "FunLabyrinthe editor"
    width = 600
    height = 500
    scene = new Scene {
      content = new BorderPane {
        // TODO Fit size to scene size
        prefWidth = 550
        prefHeight = 450

        top = mainMenu
        center = mainTabPane
      }
    }
  }

  lazy val mainMenu: MenuBar = {
    new MenuBar {
      menus = List(
          new Menu {
            text = "File"
            items = List(
                new MenuItem {
                  text = "Close"
                  onAction = stage.close
                })
          })
    }
  }

  lazy val mainTabPane: TabPane = {
    new TabPane {
      tabs = List(mapEditorTab)
    }
  }

  lazy val mapEditorTab: Tab = {
    new Tab {
      text = "Maps"
      closable = false
      content = new MapEditorPane
    }
  }
}
