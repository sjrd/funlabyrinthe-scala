package com.funlabyrinthe.editor.renderer

import com.funlabyrinthe.core.input.MouseEvent

import com.funlabyrinthe.graphics.html.Conversions

import com.raquo.laminar.api.L.{*, given}

import be.doeraene.webcomponents.ui5
import be.doeraene.webcomponents.ui5.configkeys.{BarDesign, ButtonDesign, IconName, ListMode}

import com.funlabyrinthe.editor.renderer.domext.ImageBitmapRenderingContext

import com.funlabyrinthe.editor.renderer.UniverseInterface.*
import com.funlabyrinthe.editor.renderer.LaminarUtils.*
import com.funlabyrinthe.editor.renderer.inspector.InspectedObject.PropSetEvent

import com.funlabyrinthe.coreinterface.EditableMap
import com.funlabyrinthe.coreinterface.EditableMap.ResizingDirection

class MapEditor(
  universeIntf: Signal[UniverseInterface],
  universeIntfUIState: Var[UniverseInterface.UIState],
  setPropertyHandler: Observer[PropSetEvent[?]],
)(using ErrorHandler):
  import MapEditor.*

  private val resizingInterface: Var[Option[EditableMap.ResizingView]] = Var(None)
  private val isResizingMap = resizingInterface.signal.map(_.isDefined).distinct

  private val currentMap: Signal[EditableMap] =
    universeIntf.combineWith(resizingInterface.signal).map { (universeIntf, resizingIntf) =>
      resizingIntf.getOrElse(universeIntf.mapEditInterface)
    }

  private val currentMapInfo =
    universeIntf.combineWith(currentMap).map { (universeIntf, currentMap) =>
      UniverseInterface.Map.buildFromEditableMap(currentMap, universeIntf.uiState.currentFloor)
    }

  private def uiStateUpdater[B](f: (UIState, B) => UIState): Observer[B] =
    universeIntfUIState.updater(f)

  private def refreshUI(): Unit = universeIntfUIState.update(identity)

  private val selectedComponentChanges: Observer[Option[String]] =
    uiStateUpdater((uiState, selected) => uiState.copy(selectedComponentID = selected))

  val flatPaletteComponents: Signal[List[PaletteGroup | PaletteComponent]] =
    for intf <- universeIntf yield
      intf.paletteComponents.flatMap { paletteGroup =>
        paletteGroup :: paletteGroup.components
      }
  end flatPaletteComponents

  private def flatPaletteKeyOf(elem: PaletteGroup | PaletteComponent): (Int, String) = elem match
    case elem: PaletteGroup     => (1, elem.id)
    case elem: PaletteComponent => (2, elem.component.id)

  lazy val topElement: Element =
    ui5.TabContainer(
      ui5.Tab(
        _.text <-- currentMap.map(_.id),
        div(
          className := "map-editor-tab-content",
          componentPalette,
          mapView,
          objectInspector,
        ),
      ),
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
    val component = initial.component
    ui5.UList.customItem(
      className := "component-button",
      dataAttr("componentid") := component.id,
      title := component.id,
      _.selected <-- signal.map(_.selected),
      canvasTag(
        className := "component-button-icon",
        width := ComponentIconSize.px,
        height := ComponentIconSize.px,
        drawFromSignal(signal.map(_.component.drawIcon())),
        onClick.filter(_ => component.isComponentCreator).stopPropagation --> { e =>
          val createdComponent = component.createNewComponent()
          universeIntfUIState.update { prev =>
            prev.copy(selectedComponentID = Some(createdComponent.id))
          }
        },
      )
    )
  end componentButton

  private lazy val mapView: Element =
    div(
      className := "map-view",
      div(
        className := "editing-map",
        className <-- isResizingMap.map { resizing =>
          if resizing then "editing-map-resizing"
          else "editing-map-not-resizing"
        },
        canvasTag(
          width <-- currentMapInfo.map(_.currentFloorRect._1.px),
          height <-- currentMapInfo.map(_.currentFloorRect._2.px),
          drawFromSignal(currentMapInfo.combineWith(currentMap).map((info, map) => map.drawFloor(info.currentFloor))),
          onClick.mapToEvent.compose(_.withCurrentValueOf(universeIntf, currentMap)) --> { (event, universeIntf, map) =>
            val coreEvent = Conversions.htmlMouseEvent2core(event)
            universeIntf.mouseClickOnMap(map, coreEvent)
            refreshUI()
          },
        ),
        children <-- isResizingMap.map { resizing =>
          if !resizing then
            Nil
          else
            for
              side <- MapSide.values.toList
              grow <- List(true, false)
            yield
              val towardsSide =
                if grow then side
                else MapSide.values((side.ordinal + 2) % 4)
              val iconName: IconName = towardsSide match
                case MapSide.North => IconName.`slim-arrow-up`
                case MapSide.East  => IconName.`slim-arrow-right`
                case MapSide.South => IconName.`slim-arrow-down`
                case MapSide.West  => IconName.`slim-arrow-left`

              ui5.Button(
                className := s"resize-button",
                className := s"resize-button-${if side.vertical then "vertical" else "horizontal"}",
                className := s"resize-button-${if grow then "grow" else "shrink"}",
                className := s"resize-button-${side.toString().toLowerCase()}",
                _.design := (if grow then ButtonDesign.Positive else ButtonDesign.Negative),
                _.iconOnly := true,
                _.icon := iconName,
                _.disabled <-- currentMap.sample(resizingInterface).map { optResizingIntf =>
                  !optResizingIntf.exists(intf => intf.canResize(side.toResizingDirection, grow))
                },
                _.events.onClick.compose(_.sample(resizingInterface)) --> { (optResizingIntf) =>
                  for resizingIntf <- optResizingIntf do
                    resizingIntf.resize(side.toResizingDirection, grow)
                    refreshUI()
                },
              )
            end for
          end if
        },
      ),
      ui5.Bar(
        className := "map-view-toolbar",
        _.design := BarDesign.Footer,
        _.slots.endContent <-- resizingInterface.signal.map { (optResizingIntf) =>
          optResizingIntf match
            case None =>
              Seq(
                ui5.Button(
                  _.icon := IconName.resize,
                  "Resize map",
                  _.events.onClick.compose(_.sample(currentMap).map(map => Some(map.newResizingView()))) --> resizingInterface.writer,
                ),
              )
            case Some(resizingIntf) =>
              Seq(
                ui5.Button(
                  _.icon := IconName.accept,
                  _.design := ButtonDesign.Positive,
                  "Confirm new size",
                  _.events.onClick.mapToUnit --> { () =>
                    resizingIntf.commit()
                    resizingInterface.set(None)
                  },
                ),
                ui5.Button(
                  _.icon := IconName.cancel,
                  _.design := ButtonDesign.Negative,
                  "Cancel resizing",
                  _.events.onClick.mapTo(None) --> resizingInterface.writer,
                ),
              )
          end match
        },
        _.slots.endContent := ui5.Label(
          _.id := "floor-selector-label",
          _.forId := "floor-selector",
          _.showColon := true,
          "Floor",
        ),
        _.slots.endContent <-- isResizingMap.map { isResizing =>
          if !isResizing then
            Nil
          else
            List(
              ui5.Button(
                _.design := ButtonDesign.Positive,
                _.iconOnly := true,
                _.icon := IconName.`slim-arrow-down`,
                _.tooltip := "Add a floor to the bottom",
                _.disabled <-- currentMapInfo.combineWith(resizingInterface).map { (mapInfo, optResizingIntf) =>
                  !optResizingIntf.exists { intf =>
                    mapInfo.currentFloor == 0 && intf.canResize("down", grow = true)
                  }
                },
                _.events.onClick.compose(_.sample(resizingInterface)) --> { optResizingIntf =>
                  for resizingIntf <- optResizingIntf do
                    resizingIntf.resize("down", grow = true)
                    refreshUI()
                },
              ),
            )
        },
        _.slots.endContent := ui5.StepInput(
          _.id := "floor-selector",
          _.accessibleNameRef := "floor-selector-label",
          _.min := 0,
          _.max <-- currentMapInfo.map(_.floors - 1),
          _.disabled <-- currentMapInfo.map(_.floors < 2),
          _.value <-- currentMapInfo.map(_.currentFloor),
          textAlign := "center",
          _.events.onChange.map(_.target.value.toInt) --> uiStateUpdater[Int]((s, floor) => s.copy(currentFloor = floor)),
        ),
        _.slots.endContent <-- isResizingMap.map { isResizing =>
          if !isResizing then
            Nil
          else
            List(
              ui5.Button(
                _.design := ButtonDesign.Positive,
                _.iconOnly := true,
                _.icon := IconName.`slim-arrow-up`,
                _.tooltip := "Add a floor to the top",
                _.disabled <-- currentMapInfo.combineWith(resizingInterface).map { (mapInfo, optResizingIntf) =>
                  !optResizingIntf.exists { intf =>
                    mapInfo.currentFloor == (intf.floors - 1) && intf.canResize("up", grow = true)
                  }
                },
                _.events.onClick.compose(_.sample(resizingInterface)) --> { optResizingIntf =>
                  for resizingIntf <- optResizingIntf do
                    resizingIntf.resize("up", grow = true)
                    val newFloor = resizingIntf.floors - 1
                    universeIntfUIState.update(_.copy(currentFloor = newFloor))
                },
              ),
              ui5.Button(
                _.design := ButtonDesign.Negative,
                _.iconOnly := true,
                _.icon := IconName.delete,
                _.tooltip := "Delete the current floor",
                _.disabled <-- currentMapInfo.combineWith(resizingInterface).map { (mapInfo, optResizingIntf) =>
                  !optResizingIntf.exists { intf =>
                    if mapInfo.currentFloor == 0 then intf.canResize("down", grow = false)
                    else if mapInfo.currentFloor == (intf.floors - 1) then intf.canResize("up", grow = false)
                    else false
                  }
                },
                _.events.onClick.compose(_.sample(currentMapInfo.combineWith(resizingInterface))) --> { (mapInfo, optResizingIntf) =>
                  for resizingIntf <- optResizingIntf do
                    val direction: ResizingDirection = if mapInfo.currentFloor == 0 then "down" else "up"
                    resizingIntf.resize(direction, grow = false)
                    val newFloor = if mapInfo.currentFloor == 0 then 0 else resizingIntf.floors - 1
                    universeIntfUIState.update(_.copy(currentFloor = newFloor))
                },
              ),
            )
        },
      ),
    )
  end mapView

  private lazy val objectInspector: Element =
    div(
      className := "object-inspector-column",
      new inspector.ObjectInspector(
        universeIntf.map(_.selectedComponentInspected),
        setPropertyHandler,
      ).topElement,
    )
  end objectInspector
end MapEditor

object MapEditor:
  private enum MapSide(val vertical: Boolean, val toResizingDirection: ResizingDirection):
    case North extends MapSide(vertical = true, "north")
    case East extends MapSide(vertical = false, "east")
    case South extends MapSide(vertical = true, "south")
    case West extends MapSide(vertical = false, "west")
  end MapSide
end MapEditor
