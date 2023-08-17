package com.funlabyrinthe.editor.renderer

import scala.concurrent.ExecutionContext.Implicits.global

import scala.scalajs.js

import org.scalajs.dom

import com.funlabyrinthe.core.*
import com.funlabyrinthe.core.input.MouseEvent

import com.raquo.laminar.api.L.{*, given}

import be.doeraene.webcomponents.ui5
import be.doeraene.webcomponents.ui5.configkeys.IconName

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

  locally {
    // Work around the initial loading time for images
    js.timers.setTimeout(200) {
      universeIntfVar.now().updated.foreach(universeIntfVar.set(_))
    }
  }

  lazy val topElement: Element =
    div(
      menu,
      tabs,
    )
  end topElement

  private lazy val menu =
    // feed the bus to open the menu at the fed element
    val openMenuBus: EventBus[dom.HTMLElement] = new EventBus

    div(
      ui5.Button("File", _.events.onClick.map(_.target) --> openMenuBus.writer),
      ui5.Menu(
        inContext { el =>
          openMenuBus.events.map(el.ref -> _) --> Observer[(ui5.Menu.Ref, dom.HTMLElement)](_.showAt(_))
        },
        _.item(_.text := "Save", _.icon := IconName.save),
        _.item(_.text := "Exit", _.icon := IconName.`journey-arrive`),
        _.events.onItemClick.compose(_.withCurrentValueOf(universeIntf)) --> { (event, intf) =>
          event.detail.text match
            case "Save" => save(intf)
            case "Exit" => exit()
        },
      ),
    )
  end menu

  private lazy val tabs =
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
  end tabs

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

  private def save(intf: UniverseInterface): Unit =
    universeFile.save().onComplete(println(_))
  end save

  private def exit(): Unit =
    dom.window.close()
  end exit
end UniverseEditor
