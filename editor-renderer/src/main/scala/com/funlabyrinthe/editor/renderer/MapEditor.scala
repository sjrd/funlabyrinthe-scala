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

class MapEditor(
  universeIntf: Signal[UniverseInterface],
  mapMouseClicks: Observer[MouseEvent],
  universeIntfUIState: Var[UniverseInterface.UIState],
  setPropertyHandler: Observer[PropSetEvent],
)(using ErrorHandler):
  import MapEditor.*

  private val currentMap = universeIntf.map(_.map)

  private val isResizingMap: Var[Boolean] = Var(false)

  private def uiStateUpdater[B](f: (UIState, B) => UIState): Observer[B] =
    universeIntfUIState.updater(f)

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
    case elem: PaletteComponent => (2, elem.componentID)

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
      className := "map-view",
      div(
        className := "editing-map",
        className <-- isResizingMap.signal.map { resizing =>
          if resizing then "editing-map-resizing"
          else "editing-map-not-resizing"
        },
        canvasTag(
          width <-- currentMap.map(_.currentFloorRect._1.px),
          height <-- currentMap.map(_.currentFloorRect._2.px),
          drawFromSignal(currentMap.map(_.floorImage)),
          onClick.mapToEvent.map(Conversions.htmlMouseEvent2core(_)) --> mapMouseClicks,
        ),
        children <-- isResizingMap.signal.map { resizing =>
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
              )
            end for
          end if
        },
      ),
      ui5.Bar(
        className := "map-view-toolbar",
        _.design := BarDesign.Footer,
        _.slots.endContent <-- isResizingMap.signal.map { resizing =>
          if !resizing then
            Seq(
              ui5.Button(
                _.icon := IconName.resize,
                "Resize map",
                _.events.onClick.mapTo(true) --> isResizingMap.writer,
              ),
            )
          else
            Seq(
              ui5.Button(
                _.icon := IconName.accept,
                _.design := ButtonDesign.Positive,
                "Confirm new size",
                _.events.onClick.mapTo(false) --> isResizingMap.writer,
              ),
              ui5.Button(
                _.icon := IconName.cancel,
                _.design := ButtonDesign.Negative,
                "Cancel resizing",
                _.events.onClick.mapTo(false) --> isResizingMap.writer,
              ),
            )
          end if
        },
        _.slots.endContent := ui5.Label(
          _.id := "floor-selector-label",
          _.forId := "floor-selector",
          _.showColon := true,
          "Floor",
        ),
        _.slots.endContent := ui5.StepInput(
          _.id := "floor-selector",
          _.accessibleNameRef := "floor-selector-label",
          _.min := 0,
          _.max <-- universeIntf.map(_.map.floors - 1),
          _.disabled <-- universeIntf.map(_.map.floors < 2),
          _.value <-- universeIntf.map(_.map.currentFloor),
          _.events.onChange.map(_.target.value.toInt) --> uiStateUpdater[Int]((s, floor) => s.copy(currentFloor = floor)),
        ),
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
  private enum MapSide(val vertical: Boolean):
    case North extends MapSide(vertical = true)
    case East extends MapSide(vertical = false)
    case South extends MapSide(vertical = true)
    case West extends MapSide(vertical = false)
  end MapSide
end MapEditor
