package com.funlabyrinthe.editor

import com.funlabyrinthe._
import core._
import core.graphics._

import scalafx.Includes._
import scalafx.stage.Stage
import scalafx.scene.layout._
import scalafx.scene.control._

import scalafx.geometry.Orientation

class UniverseEditor(stage: Stage, val universeFile: UniverseFile) extends BorderPane {
  val universe = universeFile.universe

  top = mainMenu
  center = mainTabPane

  private lazy val mainMenu: MenuBar = {
    new MenuBar {
      menus = List(
        new Menu {
          text = "File"
          items = List(
            new MenuItem {
              text = "Save"
              onAction = () => saveFile()
            },
            new MenuItem {
              text = "Close"
              onAction = () => stage.close()
            },
          )
        },
      )
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
      content = new MapEditorPane()(universe)
    }
  }

  def saveFile(): Unit =
    universeFile.save()
  end saveFile
}
