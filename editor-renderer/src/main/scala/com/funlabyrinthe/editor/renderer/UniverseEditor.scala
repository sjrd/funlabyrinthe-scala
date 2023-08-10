package com.funlabyrinthe.editor.renderer

import scala.concurrent.ExecutionContext.Implicits.global

import com.funlabyrinthe.core.*
import com.funlabyrinthe.core.input.MouseEvent

import com.raquo.laminar.api.L.{*, given}
import be.doeraene.webcomponents.ui5

class UniverseEditor(val universeFile: UniverseFile):
  val universeIntfVar =
    Var({
      val universe = universeFile.universe
      val mapID = universe.components[EditableMap].head.id
      val currentFloor = 0
      val selectedComponentID = "Wall"
      UniverseInterface(universe, mapID, currentFloor, selectedComponentID)
    })

  val universeIntf = universeIntfVar.signal

  val mapMouseClickBus = new EventBus[MouseEvent]

  lazy val topElement: Element =
    ui5.TabContainer(
      mapMouseClickBus.events.withCurrentValueOf(universeIntf) --> { (event, intf) =>
        for result <- intf.mouseClickOnMap(event) do
          universeIntfVar.set(result)
      },
      mapEditorTab,
      ui5.Tab(
        _.text := "Sources",
        p("Pseudo sources"),
      )
    )
  end topElement

  private lazy val mapEditor = new MapEditor(universeIntf, mapMouseClickBus.writer)

  private lazy val mapEditorTab: Element =
    ui5.Tab(
      _.text := "Maps",
      mapEditor.topElement,
    )
end UniverseEditor
