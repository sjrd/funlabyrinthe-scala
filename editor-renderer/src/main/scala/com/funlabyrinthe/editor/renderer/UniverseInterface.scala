package com.funlabyrinthe.editor.renderer

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import com.funlabyrinthe.core.input.{MouseButton, MouseEvent}

import com.funlabyrinthe.coreinterface.*
import com.funlabyrinthe.coreinterface as intf

import org.scalajs.dom.CanvasRenderingContext2D
import org.scalajs.dom.ImageBitmap
import org.scalajs.dom.OffscreenCanvas

import com.funlabyrinthe.editor.renderer.inspector.{InspectedObject, *}

final class UniverseInterface(
  universe: Universe,
  val uiState: UniverseInterface.UIState,
):
  import UniverseInterface.*
  import uiState.*

  val paletteComponents: List[PaletteGroup] =
    val groups1 = universe.allEditableComponents().groupMap(c => (c.category.id, c.category.name)) { component =>
      PaletteComponent(component)
    }
    val groups2 =
      for ((categoryID, categoryName), paletteComponents) <- groups1 yield
        PaletteGroup(categoryID, categoryName, paletteComponents.toList)
    groups2.toList
  end paletteComponents

  val mapEditInterface = universe.getEditableMapByID(mapID).get

  val selectedComponentInspected: InspectedObject =
    buildInspectedObject(universe, selectedComponentID)

  def mouseClickOnMap(editableMap: EditableMap, event: MouseEvent): Unit =
    selectedComponentID match
      case Some(selectedID) if event.button == MouseButton.Primary =>
        val selectedComponent = universe.getEditableComponentByID(selectedID).get
        editableMap.onMouseClicked(event.x, event.y, currentFloor, selectedComponent)
      case _ =>
        ()
  end mouseClickOnMap
end UniverseInterface

object UniverseInterface:
  inline val ComponentIconSize = 30

  final case class UIState(
    mapID: String,
    currentFloor: Int,
    selectedComponentID: Option[String],
  )

  object UIState:
    def defaultFor(universe: Universe): UIState =
      UIState(universe.allEditableMaps().head.id, 0, None)
  end UIState

  final class PaletteGroup(val id: String, val title: String, val components: List[PaletteComponent])

  final class PaletteComponent(val component: EditableComponent)

  final class Map(
    val id: String,
    val floors: Int,
    val currentFloor: Int,
    val currentFloorRect: (Double, Double),
  )

  object Map:
    def buildFromUniverse(universe: Universe, mapID: String, currentFloor: Int): Map =
      val underlying = universe.getEditableMapByID(mapID).get
      buildFromEditableMap(underlying, currentFloor)

    def buildFromEditableMap(underlying: EditableMap, currentFloor: Int): Map =
      val floors = underlying.floors
      val dimensions = underlying.getFloorRect(currentFloor)
      val currentFloorRect = (dimensions.width, dimensions.height)
      Map(underlying.id, floors, currentFloor, currentFloorRect)
    end buildFromEditableMap
  end Map

  private def buildInspectedObject(universe: Universe, componentID: Option[String]): InspectedObject =
    import InspectedObject.*

    componentID.flatMap(universe.getEditableComponentByID(_).toOption) match
      case None =>
        InspectedObject(Nil)

      case Some(coreComponent) =>
        InspectedObject(coreComponent.inspect().properties.toList.map(convertInspectedProperty(_)))
  end buildInspectedObject

  private def convertInspectedProperty(prop: intf.InspectedObject.InspectedProperty): InspectedObject.InspectedProperty =
    val convertedEditor = prop.editor match
      case intf.InspectedObject.PropertyEditor.StringValue() =>
        InspectedObject.PropertyEditor.StringValue

      case intf.InspectedObject.PropertyEditor.BooleanValue() =>
        InspectedObject.PropertyEditor.BooleanValue

      case intf.InspectedObject.PropertyEditor.StringChoices(choices) =>
        InspectedObject.PropertyEditor.StringChoices(choices.toList)

      case intf.InspectedObject.PropertyEditor.PainterValue() =>
        InspectedObject.PropertyEditor.PainterEditor
    end convertedEditor

    InspectedObject.InspectedProperty(prop.name, prop.stringRepr, convertedEditor, prop.setStringRepr)
  end convertInspectedProperty
end UniverseInterface
