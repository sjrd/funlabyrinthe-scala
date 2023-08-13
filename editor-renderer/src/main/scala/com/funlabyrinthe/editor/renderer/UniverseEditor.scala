package com.funlabyrinthe.editor.renderer

import scala.concurrent.ExecutionContext.Implicits.global

import com.funlabyrinthe.core.*
import com.funlabyrinthe.core.input.MouseEvent

import com.raquo.laminar.api.L.{*, given}
import be.doeraene.webcomponents.ui5
import com.funlabyrinthe.editor.renderer.inspector.InspectedObject.InspectedProperty
import com.funlabyrinthe.editor.renderer.inspector.InspectedObject.PropSetEvent

class UniverseEditor(val universeFile: UniverseFile):
  val universeIntfVar =
    Var({
      val universe = universeFile.universe
      val mapID = universe.components[EditableMap].head.id
      val currentFloor = 0
      UniverseInterface(universe, mapID, currentFloor, None)
    })

  val universeIntf = universeIntfVar.signal

  val mapMouseClickBus = new EventBus[MouseEvent]
  val setPropertyBus = new EventBus[PropSetEvent]

  lazy val topElement: Element =
    ui5.TabContainer(
      mapMouseClickBus.events.withCurrentValueOf(universeIntf) --> { (event, intf) =>
        for result <- intf.mouseClickOnMap(event) do
          universeIntfVar.set(result)
      },
      setPropertyBus.events.withCurrentValueOf(universeIntf) --> { (event, intf) =>
        event.prop.setStringRepr(event.newValue)
        for result <- intf.updated do
          universeIntfVar.set(result)
      },
      mapEditorTab,
      ui5.Tab(
        _.text := "Sources",
        p("Pseudo sources"),
      )
    )
  end topElement

  private lazy val mapEditor =
    new MapEditor(
      universeIntf,
      mapMouseClickBus.writer,
      universeIntfVar.updater(_.withSelectedComponentID(_)),
      setPropertyBus.writer,
    )
  end mapEditor

  private lazy val mapEditorTab: Element =
    ui5.Tab(
      _.text := "Maps",
      mapEditor.topElement,
    )
end UniverseEditor
