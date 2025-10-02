package com.funlabyrinthe.editor.renderer

import scala.scalajs.js

import org.scalajs.dom

import com.raquo.laminar.api.L.{*, given}

import be.doeraene.webcomponents.ui5
import be.doeraene.webcomponents.ui5.configkeys.{BarDesign, ButtonDesign, IconName, ListMode}

import com.funlabyrinthe.editor.renderer.electron.fileService

import com.funlabyrinthe.editor.renderer.domext.ImageBitmapRenderingContext

import com.funlabyrinthe.editor.renderer.UniverseInterface.*
import com.funlabyrinthe.editor.renderer.LaminarUtils.*
import com.funlabyrinthe.editor.renderer.inspector.InspectedObject.PropSetEvent

import com.funlabyrinthe.coreinterface.EditableMap
import com.funlabyrinthe.coreinterface.EditableMap.ResizingDirection
import com.funlabyrinthe.coreinterface.EditingServices
import com.funlabyrinthe.coreinterface.Universe

class UniverseEditor(
  project: Project,
  universe: Universe,
)(using ErrorHandler, Dialogs)
    extends Editor(project):

  import UniverseEditor.*

  val tabTitle = "Maps"

  val isModifiedVar: Var[Boolean] = Var(false)
  val isModified: Signal[Boolean] = isModifiedVar.signal
  val universeModifications = isModifiedVar.writer.contramap((u: Unit) => true)

  private val universeIntfUIState: Var[UniverseInterface.UIState] =
    Var(UniverseInterface.UIState.defaultFor(universe))

  private val universeIntf = universeIntfUIState.signal.map(UniverseInterface(universe, _))

  private val setPropertyBus = new EventBus[PropSetEvent[?]]

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

  private val mapInfoAtCursorVar: Var[String] = Var("")
  private val mapInfoAtCursor = mapInfoAtCursorVar.signal

  private def uiStateUpdater[B](f: (UIState, B) => UIState): Observer[B] =
    universeIntfUIState.updater(f)

  def refreshUI(): Unit = universeIntfUIState.update(identity)

  private def markModified(): Unit = universeModifications.onNext(())

  private val selectedComponentChanges: Observer[Option[String]] =
    uiStateUpdater((uiState, selected) => uiState.copy(selectedComponentID = selected))

  private val editingServices: EditingServices = new {
    def markModified(): Unit =
      refreshUI()
      UniverseEditor.this.markModified()

    def askConfirmation(message: String): js.Promise[Boolean] =
      refreshUI()
      JSPI.async {
        Dialogs.askConfirmation(message)
      }

    def cancel(): Nothing =
      UserCancelException.cancel()

    def error(message: String): Nothing =
      throw UserErrorMessage(message)
  }

  def saveContent(): Unit =
    val universePickleString = universe.save() + "\n"

    JSPI.await(fileService.saveUniverseFile(project.projectID.id, universePickleString))
    isModifiedVar.set(false)
  end saveContent

  lazy val topElement: Signal[Element] =
    Signal.fromValue(
      div(
        cls := "map-editor-container",
        ui5.TabContainer(
          setPropertyBus.events.compose(_.withCurrentValueOf(universeIntf)) --> { (event, universeIntf) =>
            ErrorHandler.handleErrors {
              event.prop.setEditorValue(event.newValue)
              markModified()

              // If we just changed the ID of the selected component, adjust the selectedComponentID
              val newSelectedComponentID = universeIntf.selectedComponent.map(_.fullID)
              if universeIntf.uiState.selectedComponentID != newSelectedComponentID then
                selectedComponentChanges.onNext(newSelectedComponentID)
              else
                // Otherwise, refresh the UI normally
                refreshUI()
            }
          },
          children <-- universeIntf.map(_.mapIDs).distinct.map(_.map { case (shortID, fullID) =>
            ui5.Tab(_.text := shortID)
          }),
          _.events.onTabSelect.stopPropagation.map(_.detail.tabIndex).compose(_.withCurrentValueOf(universeIntfUIState.signal, universeIntf)) -->
            universeIntfUIState.writer.contramap { (params: (Int, UIState, UniverseInterface)) =>
              val (tabIndex, state, intf) = params
              state.copy(mapID = intf.mapIDs(tabIndex)._2)
            },
        ),
        mapTabContent,
      )
    )
  end topElement

  private lazy val mapTabContent: Element =
    div(
      className := "map-editor-tab-content",
      componentPalette,
      mapView,
      objectInspector,
    )
  end mapTabContent

  private lazy val componentPalette: Element =
    ui5.UList(
      className := "component-palette",
      _.selectionMode := ListMode.Single,
      _.events.onSelectionChange
        .map(_.detail.maybeSelectedItem.flatMap(_.dataset.get("componentid"))) --> selectedComponentChanges,
      children <-- universeIntf.map(_.paletteComponents).split(_.id) { (key, initial, group) =>
        componentGroup(initial, group)
      },
    )
  end componentPalette

  private def componentGroup(initial: PaletteGroup, signal: Signal[PaletteGroup]): HtmlElement =
    ui5.UList.grouped(
      _.headerText := initial.title,
      children <-- signal.map(_.components).split(_.component.fullID) { (key, initial, component) =>
        componentButton(initial, component)
      },
      hackElemInsideShadowRoot("ul") { ul =>
        /* Add the attribute `part="list"` to the underlying <ul> tag of this web component.
         * This is necessary for our CSS to be able to set it to flex+wrap. Adding that
         * style to the host element ui5.UList.grouped does not have the effect we want.
         * Ideally the developers of UI5 would have provided the `list` part themselves,
         * but they chose not to, so we hack our way in.
         */
        ul.setAttribute("part", "list")
      },
    )
  end componentGroup

  private def componentButton(initial: PaletteComponent, signal: Signal[PaletteComponent]): HtmlElement =
    val component = initial.component
    ui5.UList.customItem(
      className := "component-button",
      dataAttr("componentid") := component.fullID,
      title := component.shortID,
      _.selected <-- signal.map(_.selected),
      canvasTag(
        className := "component-button-icon",
        width := ComponentIconSize.px,
        height := ComponentIconSize.px,
        drawFromSignal(signal.map(_.component.drawIcon())),
        onClick.filter(_ => component.isComponentCreator).stopPropagation --> { e =>
          val createdComponent = component.createNewComponent()
          universeIntfUIState.update { prev =>
            prev.copy(selectedComponentID = Some(createdComponent.fullID))
          }
        },
      )
    )
  end componentButton

  extension (event: dom.MouseEvent)
    private def offsets: (Double, Double) =
      val offsetX = event.asInstanceOf[js.Dynamic].offsetX.asInstanceOf[Double]
      val offsetY = event.asInstanceOf[js.Dynamic].offsetY.asInstanceOf[Double]
      (offsetX, offsetY)
  end extension

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
            if event.button == 0 then // primary button
              val (offsetX, offsetY) = event.offsets
              ErrorHandler.handleErrors {
                universeIntf.mouseClickOnMap(map, offsetX, offsetY, editingServices)
                refreshUI()
              }
          },
          onMouseMove.mapToEvent.compose(_.withCurrentValueOf(universeIntfUIState, currentMap)) --> { (event, uiState, map) =>
            val (offsetX, offsetY) = event.offsets
            mapInfoAtCursorVar.set(map.getDescriptionAt(offsetX, offsetY, uiState.currentFloor))
          },
          onMouseLeave.mapTo("") --> mapInfoAtCursorVar,
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
        _.slots.startContent := div(
          span(
            className := "map-view-map-info-at-cursor",
            text <-- mapInfoAtCursor,
          ),
        ),
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
                  _.events.onClick.mapToUnit --> { _ =>
                    resizingIntf.commit()
                    markModified()
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
    val objectInspector =
      new inspector.ObjectInspector(
        universeIntf.map(_.selectedComponentInspected),
        setPropertyBus.writer,
      )
    div(
      className := "object-inspector-column",
      objectInspector.painterEditorDialog,
      objectInspector.topElement,
      ui5.Bar(
        className := "object-inspector-column-toolbar",
        _.design := BarDesign.Footer,
        _.slots.endContent := ui5.Button(
          _.design := ButtonDesign.Positive,
          _.icon := IconName.copy,
          _.disabled <-- universeIntf.map(!_.selectedComponentIsCopiable),
          _.events.onClick.compose(_.sample(universeIntf)) --> { intf =>
            ErrorHandler.handleErrors {
              if !intf.selectedComponentIsCopiable then
                throw UserErrorMessage(s"This component cannot be copied")
              val createdComponent = intf.selectedComponent.get.copy()
              markModified()
              selectedComponentChanges.onNext(Some(createdComponent.fullID))
            }
          },
        ),
        _.slots.endContent := ui5.Button(
          _.design := ButtonDesign.Negative,
          _.icon := IconName.delete,
          _.disabled <-- universeIntf.map(!_.selectedComponentIsDestroyable),
          _.events.onClick.compose(_.sample(universeIntf)) --> { intf =>
            ErrorHandler.handleErrors {
              if !intf.selectedComponentIsDestroyable then
                throw UserErrorMessage(s"This component cannot be deleted")
              val errors = intf.selectedComponent.get.destroy()
              if errors.nonEmpty then
                throw UserErrorMessage(
                  "This component cannot be deleted at the moment",
                  picklingErrors = errors.toList.map(PicklingError.fromInterface(_)),
                )
              markModified()
              selectedComponentChanges.onNext(None)
            }
          },
        ),
      ),
    )
  end objectInspector
end UniverseEditor

object UniverseEditor:
  private enum MapSide(val vertical: Boolean, val toResizingDirection: ResizingDirection):
    case North extends MapSide(vertical = true, "north")
    case East extends MapSide(vertical = false, "east")
    case South extends MapSide(vertical = true, "south")
    case West extends MapSide(vertical = false, "west")
  end MapSide
end UniverseEditor
