package com.funlabyrinthe.editor

import com.funlabyrinthe._
import core._
import core.graphics.{ Canvas => _, _ }

import com.funlabyrinthe.graphics.{ jfx => gjfx }
import gjfx.CanvasWrapper
import gjfx.Conversions._

import scalafx.Includes._
import scalafx.scene.canvas._
import scalafx.scene.layout._
import scalafx.scene.control._
import scalafx.geometry.Orientation

import javafx.scene.{ control => jfxsc, input => jfxsi }
import javafx.{ event => jfxe }

class MapEditorPane(implicit val universe: Universe) extends SplitPane {
  orientation = Orientation.Horizontal
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
      minWidth = 150
      prefWidth = 500
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
      onMouseClicked = new jfxe.EventHandler[jfxsi.MouseEvent] {
        override def handle(event: jfxsi.MouseEvent): Unit = {
          mouseClicked(event)
        }
      }

      def mouseClicked(event: jfxsi.MouseEvent): Unit = {
        val selectedComponent = componentPalette.selectedComponent.value
        if (selectedComponent.isDefined) {
          editInterface.onMouseClicked(event, currentFloor,
              selectedComponent.get)
          update()
        }
      }
    }

    lazy val coreCanvas = new CanvasWrapper(canvas)

    def update(): Unit = {
      val rect = editInterface.getFloorRect(currentFloor)
      canvas.width = rect.width
      canvas.height = rect.height

      val drawContext = new DrawContext(coreCanvas.getGraphicsContext2D(), rect)
      editInterface.drawFloor(drawContext, currentFloor)
    }
  }
}
