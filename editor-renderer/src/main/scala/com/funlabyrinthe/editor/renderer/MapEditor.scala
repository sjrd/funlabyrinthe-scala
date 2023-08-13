package com.funlabyrinthe.editor.renderer

import com.funlabyrinthe.core.*
import com.funlabyrinthe.core.input.MouseEvent

import com.funlabyrinthe.graphics.html.Conversions

import com.raquo.laminar.api.L.{*, given}

import be.doeraene.webcomponents.ui5
import be.doeraene.webcomponents.ui5.configkeys.ListMode

import com.funlabyrinthe.editor.renderer.domext.ImageBitmapRenderingContext

import com.funlabyrinthe.editor.renderer.UniverseInterface.*
import com.funlabyrinthe.editor.renderer.LaminarUtils.*

class MapEditor(
  universeIntf: Signal[UniverseInterface],
  mapMouseClicks: Observer[MouseEvent],
  selectedComponentChanges: Observer[Option[String]],
):
  private val currentMap = universeIntf.map(_.map)

  val flatPaletteComponents: Signal[List[PaletteGroup | PaletteComponent]] =
    for intf <- universeIntf yield
      intf.paletteComponents.flatMap { paletteGroup =>
        paletteGroup :: paletteGroup.components
      }
  end flatPaletteComponents

  private def flatPaletteKeyOf(elem: PaletteGroup | PaletteComponent): (Int, String) = elem match
    case elem: PaletteGroup     => (1, elem.id)
    case elem: PaletteComponent => (2, elem.componentID)

  lazy val topElement: Element =
    ui5.TabContainer(
      ui5.Tab(
        _.text <-- currentMap.map(_.id),
        div(
          className := "map-editor-tab-content",
          componentPalette,
          mapView
        ),
      )
    )
  end topElement

  private lazy val componentPalette: Element =
    ui5.UList(
      className := "component-palette",
      _.mode := ListMode.SingleSelect,
      _.events.onSelectionChange
        .map(_.detail.maybeSelectedItem.flatMap(_.dataset.get("componentid"))) --> selectedComponentChanges,
      children <-- flatPaletteComponents.split(flatPaletteKeyOf(_)) { (key, initial, elem) =>
        initial match
          case initial: PaletteGroup =>
            componentGroup(initial, elem.map(_.asInstanceOf[PaletteGroup]))
          case initial: PaletteComponent =>
            componentButton(initial, elem.map(_.asInstanceOf[PaletteComponent]))
      },
      hackElemInsideShadowRoot("ul") { ul =>
        /* Add the attribute `part="list"` to the underlying <ul> tag of this web component.
         * This is necessary for our CSS to be able to set it to flex+wrap. Adding that
         * style to the host element ui5.UList does not have the effect we want.
         * Ideally the developers of UI5 would have provided the `list` part themselves,
         * but they chose not to, so we hack our way in.
         */
        ul.setAttribute("part", "list")
      },
    )
  end componentPalette

  private def componentGroup(initial: PaletteGroup, signal: Signal[PaletteGroup]): HtmlElement =
    ui5.UList.group(
      className := "component-group",
      child <-- signal.map(_.title),
    )
  end componentGroup

  private def componentButton(initial: PaletteComponent, signal: Signal[PaletteComponent]): HtmlElement =
    ui5.UList.customItem(
      className := "component-button",
      dataAttr("componentid") := initial.componentID,
      canvasTag(
        className := "component-button-icon",
        width := ComponentIconSize.px,
        height := ComponentIconSize.px,
        drawFromSignal(signal.map(_.icon)),
      )
    )
  end componentButton

  private lazy val mapView: Element =
    div(
      className := "editing-map",
      canvasTag(
        width <-- currentMap.map(_.currentFloorRect.width.px),
        height <-- currentMap.map(_.currentFloorRect.height.px),
        drawFromSignal(currentMap.map(_.floorImage)),
        onClick.mapToEvent.map(Conversions.htmlMouseEvent2core(_)) --> mapMouseClicks,
      ),
    )
  end mapView
end MapEditor
