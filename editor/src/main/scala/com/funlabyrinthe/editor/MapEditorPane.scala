package com.funlabyrinthe.editor

import com.funlabyrinthe._
import core._
import core.graphics._

import scalafx.scene.layout._
import scalafx.scene.control._

import scalafx.geometry.Orientation

class MapEditorPane(implicit val universe: Universe) extends SplitPane {
  orientation = Orientation.HORIZONTAL
  items.addAll(componentPalette, mapsTabPane, objectInspectorPane)

  lazy val componentPalette: ComponentPalette = new ComponentPalette {
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
