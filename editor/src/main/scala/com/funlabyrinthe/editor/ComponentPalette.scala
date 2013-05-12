package com.funlabyrinthe.editor

import com.funlabyrinthe._
import core._
import core.graphics._

import scala.collection.mutable

import scalafx.Includes._
import scalafx.scene.layout._
import scalafx.scene.control._
import scalafx.scene.image._
import scalafx.geometry._

import scalafx.beans.property.ObjectProperty

class ComponentPalette(implicit val universe: Universe) extends ScrollPane {
  import Component.{ IconWidth, IconHeight }

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

  private val ButtonIconPadding = 2
  private val ButtonWidth = IconWidth + 2*ButtonIconPadding
  private val ButtonHeight = IconHeight + 2*ButtonIconPadding

  buildContent()

  private def buildContent() {
    // TODO Make the titledPanes adapt their height upon resize
    for (component <- universe.allComponents) {
      val category = component.category
      val (_, pane) = categoriesPanes.getOrElseUpdate(category, {
        val tilePane = new TilePane {
          orientation = Orientation.HORIZONTAL
          tileAlignment = Pos.CENTER
          prefColumns = 5
          prefTileWidth = ButtonWidth + 2
          prefTileHeight = ButtonHeight + 2
        }
        val titledPane = new TitledPane {
          text = category.text
          content = tilePane
          minWidth = 40
        }
        categoriesContainer.content.add(titledPane)
        (titledPane, tilePane)
      })

      pane.content.add(new ComponentButton(component).delegate)
    }
  }

  private class ComponentButton(val component: Component) extends Button {
    val iconCanvas = new Canvas(IconWidth, IconHeight)

    {
      val drawContext = new DrawContext(iconCanvas.graphicsContext2D,
          new Rectangle2D(ButtonIconPadding, ButtonIconPadding,
              IconWidth, IconHeight))
      component.drawIcon(drawContext)
    }

    prefWidth = ButtonWidth
    prefHeight = ButtonHeight
    minWidth = ButtonWidth
    minHeight = ButtonHeight
    maxWidth = ButtonWidth
    maxHeight = ButtonHeight

    graphic = iconCanvas
    tooltip = component.id

    styleClass += "component-palette-button"

    onAction = {
      selectedComponent = Some(component)
    }
  }
}
