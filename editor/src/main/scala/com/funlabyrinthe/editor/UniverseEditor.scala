package com.funlabyrinthe.editor

import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files

import scala.collection.mutable

import com.funlabyrinthe._
import core._
import core.graphics._

import scalafx.Includes._
import scalafx.stage.Stage
import scalafx.scene.layout._
import scalafx.scene.control._

import scalafx.geometry.Orientation
import scalafx.stage.FileChooser

class UniverseEditor(stage: Stage, val universeFile: UniverseFile) extends BorderPane {
  val universe = universeFile.universe

  private val sourceEditorTabs = mutable.LinkedHashMap.empty[String, Tab]

  top = mainMenu
  center = mainTabPane

  updateSourcesMenu()

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
        new Menu {
          text = "Sources"
          items = List(
            new MenuItem {
              text = "New"
              onAction = () => newSource()
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

  def updateSourcesMenu(): Unit =
    val sourcesMenu = mainMenu.menus.find(_.text.value == "Sources").get
    sourcesMenu.items.takeInPlace(1)
    if universeFile.sourceFiles.nonEmpty then
      sourcesMenu.items += new MenuItem {
        text = "-"
      }
      for sourceName <- universeFile.sourceFiles do
        sourcesMenu.items += new MenuItem {
          text = sourceName
          onAction = () => openSourceFile(sourceName)
        }
    end if
  end updateSourcesMenu

  def newSource(): Unit =
    Files.createDirectories(universeFile.sourcesDirectory.toPath())
    val fileChooser = new FileChooser {
      title = "Load a project"
      extensionFilters ++= Seq(
        new FileChooser.ExtensionFilter("Scala source", "*.scala"),
      )
      initialDirectory = universeFile.sourcesDirectory
    }
    val selectedFile = fileChooser.showSaveDialog(stage)
    if selectedFile != null then
      doCreateNewSource(selectedFile)
  end newSource

  private def doCreateNewSource(sourceFile: File): Unit =
    if sourceFile.getParentFile() != universeFile.sourcesDirectory then
      throw UserErrorMessage("Please select a source file in the Sources directory")

    val sourceName = sourceFile.getName()
    val content = createContentForNewSource(sourceName)
    Files.writeString(sourceFile.toPath(), content, StandardCharsets.UTF_8)
    universeFile.sourceFiles += sourceName
    updateSourcesMenu()
    openSourceFile(sourceName)
  end doCreateNewSource

  private def openSourceFile(sourceName: String): Unit =
    val tab = sourceEditorTabs.getOrElseUpdate(sourceName, {
      val tab = new Tab {
        text = sourceName
        content = new SourceEditorPane(universeFile, sourceName)
        onClosed = () => sourceEditorTabs.remove(sourceName)
      }
      mainTabPane.tabs += tab
      tab
    })

    mainTabPane.selectionModel.value.clearAndSelect(mainTabPane.tabs.indexOf(tab))
  end openSourceFile

  private def createContentForNewSource(sourceName: String): String =
    val baseName = sourceName.stripSuffix(".scala")
    s"""package myfunlaby
      |
      |import com.funlabyrinthe.core.*
      |import com.funlabyrinthe.mazes.*
      |
      |class $baseName(universe: Universe) extends Module(universe):
      |end $baseName
      |""".stripMargin
  end createContentForNewSource
}
