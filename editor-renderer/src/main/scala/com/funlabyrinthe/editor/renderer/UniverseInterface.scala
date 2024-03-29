package com.funlabyrinthe.editor.renderer

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import scala.scalajs.js

import com.funlabyrinthe.coreinterface.*
import com.funlabyrinthe.coreinterface.InspectedObject.Serializer
import com.funlabyrinthe.coreinterface.InspectedObject.PropertyEditor.PainterValue.PainterItem as intfPainterItem
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
      PaletteComponent(component, uiState.selectedComponentID.contains(component.fullID))
    }
    val groups2 =
      for ((categoryID, categoryName), paletteComponents) <- groups1 yield
        PaletteGroup(categoryID, categoryName, paletteComponents.toList)
    groups2.toList
  end paletteComponents

  val mapEditInterface = universe.getEditableMapByID(mapID).get

  val selectedComponentInspected: InspectedObject =
    buildInspectedObject(universe, selectedComponentID)

  def mouseClickOnMap(editableMap: EditableMap, x: Double, y: Double): EditUserActionResult =
    selectedComponentID match
      case Some(selectedID) =>
        val selectedComponent = universe.getEditableComponentByID(selectedID).get
        editableMap.onMouseClicked(x, y, currentFloor, selectedComponent)
      case _ =>
        new EditUserActionResult.Unchanged {
          val kind = "unchanged"
        }
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
      UIState(universe.allEditableMaps().head.fullID, 0, None)
  end UIState

  final class PaletteGroup(val id: String, val title: String, val components: List[PaletteComponent])

  final class PaletteComponent(val component: EditableComponent, val selected: Boolean)

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
      Map(underlying.fullID, floors, currentFloor, currentFloorRect)
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

  private def convertInspectedProperty(prop: intf.InspectedObject.InspectedProperty): InspectedObject.InspectedProperty[?] =
    def build[T](
      convertedEditor: InspectedObject.PropertyEditor[T]
    )(using serializer: Serializer[T]): InspectedObject.InspectedProperty[T] =
      InspectedObject.InspectedProperty(
        prop.name,
        prop.valueDisplayString,
        convertedEditor,
        serializer.deserialize(prop.serializedEditorValue),
        newValue => prop.setSerializedEditorValue(serializer.serialize(newValue)),
      )
    end build

    prop.editor match
      case intf.InspectedObject.PropertyEditor.StringValue() =>
        build(InspectedObject.PropertyEditor.StringValue)

      case intf.InspectedObject.PropertyEditor.BooleanValue() =>
        build(InspectedObject.PropertyEditor.BooleanValue)

      case intf.InspectedObject.PropertyEditor.IntValue() =>
        build(InspectedObject.PropertyEditor.IntValue)

      case intf.InspectedObject.PropertyEditor.StringChoices(choices) =>
        build(InspectedObject.PropertyEditor.StringChoices(choices.toList))

      case intf.InspectedObject.PropertyEditor.PainterValue() =>
        build(InspectedObject.PropertyEditor.PainterEditor)

      case intf.InspectedObject.PropertyEditor.ColorValue() =>
        build(InspectedObject.PropertyEditor.ColorEditor)

      case intf.InspectedObject.PropertyEditor.FiniteSet(choices) =>
        build(InspectedObject.PropertyEditor.FiniteSet(choices.toList))
  end convertInspectedProperty

  // !!! Duplicate code with EditableComponent.scala
  private given PainterItemSerializer: intf.InspectedObject.Serializer[PainterItem] with
    def serialize(item: PainterItem): Any =
      item match
        case PainterItem.ImageDescription(name0) =>
          new intfPainterItem {
            val name = name0
          }
    end serialize

    def deserialize(serializedValue: Any): PainterItem =
      val dict = serializedValue.asInstanceOf[js.Dictionary[Any]]
      dict.get("name") match
        case Some(name: String) => PainterItem.ImageDescription(name)
        case _                  => illegalSerializedValue(serializedValue)
    end deserialize
  end PainterItemSerializer
end UniverseInterface
