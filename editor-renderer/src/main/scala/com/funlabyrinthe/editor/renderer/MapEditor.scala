package com.funlabyrinthe.editor.renderer

import com.funlabyrinthe.core.*
import com.funlabyrinthe.core.input.MouseEvent

import com.funlabyrinthe.graphics.html.Conversions

import com.raquo.laminar.api.L.{*, given}
import be.doeraene.webcomponents.ui5

import com.funlabyrinthe.editor.renderer.domext.ImageBitmapRenderingContext

class MapEditor(universeIntf: Signal[UniverseInterface], mapMouseClicks: Observer[MouseEvent]):
  private val currentMap = universeIntf.map(_.map)

  lazy val topElement: Element =
    ui5.TabContainer(
      ui5.Tab(
        _.text <-- currentMap.map(_.id),
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
      )
    )
  end topElement
end MapEditor
