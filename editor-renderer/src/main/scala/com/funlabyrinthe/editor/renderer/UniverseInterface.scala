package com.funlabyrinthe.editor.renderer

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

  val selectedComponent: Option[intf.EditableComponent] =
    selectedComponentID.flatMap(universe.getEditableComponentByID(_).toOption)

  val selectedComponentIsCopiable: Boolean =
    selectedComponent.fold(false)(_.isCopiable)

  val selectedComponentIsDestroyable: Boolean =
    selectedComponent.fold(false)(_.isDestroyable)

  val selectedComponentInspected: InspectedObject =
    buildInspectedObject(universe, selectedComponent)

  def mouseClickOnMap(editableMap: EditableMap, x: Double, y: Double,
      editingServices: EditingServices): Unit =
    selectedComponentID match
      case Some(selectedID) =>
        val selectedComponent = universe.getEditableComponentByID(selectedID).get
        JSPI.await(editableMap.onMouseClicked(x, y, currentFloor, selectedComponent, editingServices))
      case None =>
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

  private def buildInspectedObject(universe: Universe, component: Option[intf.EditableComponent]): InspectedObject =
    import InspectedObject.*

    component match
      case None =>
        InspectedObject(Nil)

      case Some(coreComponent) =>
        InspectedObject(coreComponent.inspect().properties.toList.map(convertInspectedProperty(_)))
  end buildInspectedObject

  private def convertInspectedProperty(prop: intf.InspectedObject.InspectedProperty): InspectedObject.InspectedProperty[?] =
    convertEditor(prop.editor) match
      case converted: ConvertedEditorAndSerializer[t] =>
        val serializer = converted.serializer
        InspectedObject.InspectedProperty[t](
          prop.name,
          prop.valueDisplayString,
          converted.convertedEditor,
          serializer.deserialize(prop.serializedEditorValue),
          newValue => prop.setSerializedEditorValue(serializer.serialize(newValue)),
          None,
        )
  end convertInspectedProperty

  private def convertEditor(editor: intf.InspectedObject.PropertyEditor): ConvertedEditorAndSerializer[?] =
    def result[T](convertedEditor: InspectedObject.PropertyEditor[T])(
        using serializer: Serializer[T]): ConvertedEditorAndSerializer[T] =
      ConvertedEditorAndSerializer(convertedEditor, serializer)
    end result

    editor match
      case intf.InspectedObject.PropertyEditor.StringValue() =>
        result(InspectedObject.PropertyEditor.StringValue)

      case intf.InspectedObject.PropertyEditor.BooleanValue() =>
        result(InspectedObject.PropertyEditor.BooleanValue)

      case intf.InspectedObject.PropertyEditor.IntValue() =>
        result(InspectedObject.PropertyEditor.IntValue)

      case intf.InspectedObject.PropertyEditor.StringChoices(choices) =>
        result(InspectedObject.PropertyEditor.StringChoices(choices.toList))

      case intf.InspectedObject.PropertyEditor.ItemList(elemEditor) =>
        convertEditor(elemEditor) match
          case convertedElem: ConvertedEditorAndSerializer[e] =>
            given Serializer[e] = convertedElem.serializer
            result(InspectedObject.PropertyEditor.ItemList(convertedElem.convertedEditor))

      case intf.InspectedObject.PropertyEditor.Struct(fieldNames, fieldEditors) =>
        val convertedFields = fieldEditors.map(convertEditor(_))
        result(InspectedObject.PropertyEditor.Struct(fieldNames, convertedFields.map(_.convertedEditor)))(
            using Serializer.makeTupleSerializer(convertedFields.map(_.serializer)))

      case intf.InspectedObject.PropertyEditor.PainterValue() =>
        result(InspectedObject.PropertyEditor.PainterEditor)

      case intf.InspectedObject.PropertyEditor.ColorValue() =>
        result(InspectedObject.PropertyEditor.ColorEditor)

      case intf.InspectedObject.PropertyEditor.FiniteSet(choices) =>
        result(InspectedObject.PropertyEditor.FiniteSet(choices.toList))
  end convertEditor

  private final case class ConvertedEditorAndSerializer[T](
    convertedEditor: InspectedObject.PropertyEditor[T],
    serializer: Serializer[T],
  )

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
