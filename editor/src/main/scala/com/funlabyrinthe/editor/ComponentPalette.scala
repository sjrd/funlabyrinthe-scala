package com.funlabyrinthe.editor

import com.funlabyrinthe._
import core._
import core.graphics._

import scala.collection.mutable

import scalafx.Includes._
import scalafx.scene.layout._
import scalafx.scene.control._
import scalafx.geometry._

import scalafx.beans.property.ObjectProperty

class ComponentPalette(implicit val universe: Universe) extends ScrollPane {
  hbarPolicy = ScrollPane.ScrollBarPolicy.NEVER
  fitToWidth = true

  style = "-fx-background-color: #0000ff"

  private val _selectedComponent = ObjectProperty[Option[Component]](None)
  def selectedComponent = _selectedComponent
  def selectedComponent_=(v: Option[Component]) {
    selectedComponent() = v
  }

  private val categoriesContainer = new VBox {
    fillWidth = true

    style = "-fx-background-color: #ff0000"
  }
  content = categoriesContainer

  private val categoriesPanes =
    new mutable.HashMap[ComponentCategory, (TitledPane, TilePane)]

  private val ButtonWidth = Component.IconWidth + 8
  private val ButtonHeight = Component.IconHeight + 8

  buildContent()

  private def buildContent() {
    // TODO Make the titledPanes adapt their height upon resize
    for (component <- universe.components) {
      val category = component.category
      val (_, pane) = categoriesPanes.getOrElseUpdate(category, {
        val tilePane = new TilePane {
          orientation = Orientation.HORIZONTAL
          tileAlignment = Pos.CENTER
          prefColumns = 5
        }
        val titledPane = new TitledPane {
          text = category.text
          content = tilePane
          minWidth = 40
        }
        categoriesContainer.content.add(titledPane)
        (titledPane, tilePane)
      })

      pane.content.add(new Button {
        prefWidth = ButtonWidth
        prefHeight = ButtonHeight
        minWidth = ButtonWidth
        minHeight = ButtonHeight
        maxWidth = ButtonWidth
        maxHeight = ButtonHeight

        text = component.id
        tooltip = component.id

        onAction = {
          selectedComponent = Some(component)
        }
      }.delegate)
    }
  }
}
