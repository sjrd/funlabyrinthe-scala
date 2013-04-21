package com.funlabyrinthe.editor

import scalafx.scene.layout._
import scalafx.scene.control._

import scalafx.geometry.Orientation

class MapEditorPane extends SplitPane {
  orientation = Orientation.HORIZONTAL
  items.addAll(componentPalettePane, mapsTabPane, objectInspectorPane)

  lazy val componentPalettePane: ScrollPane = {
    new ScrollPane {
      content = new VBox {
        content = new Button("Palette here")
      }
    }
  }

  lazy val mapsTabPane: TabPane = {
    new TabPane {
      tabs = List(
          new Tab {
            text = "MainMap"
          })
    }
  }

  lazy val objectInspectorPane: ScrollPane = {
    new ScrollPane {
      content = new VBox {
        content = new Button("Inspector here")
      }
    }
  }
}
