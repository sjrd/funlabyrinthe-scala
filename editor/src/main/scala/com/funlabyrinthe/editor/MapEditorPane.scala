package com.funlabyrinthe.editor

import com.funlabyrinthe._
import core._
import core.graphics._

import scalafx.Includes._
import scalafx.scene.layout._
import scalafx.scene.control._
import scalafx.geometry.Orientation

import javafx.scene.{ control => jfxsc }

class MapEditorPane(implicit val universe: Universe) extends SplitPane {
  orientation = Orientation.HORIZONTAL
  items.addAll(componentPalette, mapsTabPane, objectInspector)

  for (map <- universe.components[EditableMap]) {
    mapTabs.add(new MapEditorTab(map))
  }

  lazy val componentPalette: ComponentPalette = new ComponentPalette {
    SplitPane.setResizableWithParent(this, false)
    minWidth = 150
    prefWidth = 250
  }

  lazy val mapsTabPane: TabPane = {
    new TabPane {
    }
  }
  lazy val mapTabs = mapsTabPane.tabs

  lazy val objectInspector: ObjectInspector = new ObjectInspector {
    SplitPane.setResizableWithParent(this, false)
    minWidth = 150
    prefWidth = 250

    inspectedObject <== componentPalette.selectedComponent
  }

  private class MapEditorTab(val map: EditableMap) extends jfxsc.Tab {
    private implicit val wrapper: Tab = this
    import wrapper._

    val editInterface = map.getEditInterface()

    var currentFloor = 0 // stub

    text = map.id
    content = canvas

    update()

    lazy val canvas = new Canvas {
      onMouseClicked = { (event: javafx.scene.input.MouseEvent) =>
        val selectedComponent = componentPalette.selectedComponent.value
        if (selectedComponent.isDefined) {
          editInterface.onMouseClicked(event, selectedComponent.get)
          update()
        }
      }
    }

    def update() {
      val rect = editInterface.getFloorRect(currentFloor)
      canvas.width = rect.width
      canvas.height = rect.height

      val drawContext = new DrawContext(canvas.graphicsContext2D, rect)
      editInterface.drawFloor(drawContext, currentFloor)
    }
  }
}
