package com.funlabyrinthe.editor.renderer

import com.funlabyrinthe.core.*
import com.funlabyrinthe.core.input.MouseEvent

import com.funlabyrinthe.graphics.html.Conversions

import com.raquo.laminar.api.L.{*, given}

import be.doeraene.webcomponents.ui5
import be.doeraene.webcomponents.ui5.configkeys.ListMode

import com.funlabyrinthe.editor.renderer.domext.ImageBitmapRenderingContext

import com.funlabyrinthe.editor.renderer.UniverseInterface.*

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
          ui5.UList(
            className := "component-palette",
            _.mode := ListMode.SingleSelect,
            _.events.onSelectionChange
              .map(_.detail.maybeSelectedItem.flatMap(_.dataset.get("componentid"))) --> selectedComponentChanges,
            children <-- flatPaletteComponents.split(flatPaletteKeyOf(_)) { (key, initial, elem) =>
              initial match
                case initial: PaletteGroup =>
                  ui5.UList.group(child <-- elem.map(_.asInstanceOf[PaletteGroup].title))
                case initial: PaletteComponent =>
                  ui5.UList.item(
                    dataAttr("componentid") := initial.componentID,
                    child <-- elem.map(_.asInstanceOf[PaletteComponent].componentID),
                  )
            },
          ),
          div(
            className := "editing-map",
            canvasTag(
              width <-- currentMap.map(map => map.currentFloorRect.width.toString() + "px"),
              height <-- currentMap.map(map => map.currentFloorRect.height.toString() + "px"),
              inContext { canvasElem =>
                currentMap.map(_.floorImage) --> { image =>
                  val ctx = canvasElem.ref.getContext("bitmaprenderer").asInstanceOf[ImageBitmapRenderingContext]
                  ctx.transferFromImageBitmap(image)
                }
              },
              onClick.mapToEvent.map(Conversions.htmlMouseEvent2core(_)) --> mapMouseClicks,
            ),
          ),
        ),
      )
    )
  end topElement
end MapEditor
