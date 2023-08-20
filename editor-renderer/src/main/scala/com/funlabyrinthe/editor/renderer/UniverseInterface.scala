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
  mapID: String,
  currentFloor: Int,
  val selectedComponentID: Option[String],
):
  import UniverseInterface.*

  val paletteComponents: List[PaletteGroup] =
    val groups1 = universe.allEditableComponents().groupMap(c => (c.category.id, c.category.name)) { component =>
      PaletteComponent(component.id, component.drawIcon())
    }
    val groups2 =
      for ((categoryID, categoryName), paletteComponents) <- groups1 yield
        PaletteGroup(categoryID, categoryName, paletteComponents.toList)
    groups2.toList
  end paletteComponents

  val map = Map.buildFromUniverse(universe, mapID, currentFloor)

  val selectedComponentInspected: InspectedObject =
    buildInspectedObject(universe, selectedComponentID)

  def withSelectedComponentID(selected: Option[String]): UniverseInterface =
    new UniverseInterface(universe, mapID, currentFloor, selected)

  def mouseClickOnMap(event: MouseEvent): Future[UniverseInterface] =
    selectedComponentID match
      case Some(selectedID) if event.button == MouseButton.Primary =>
        Future {
          val selectedComponent = universe.getEditableComponentByID(selectedID).get
          val editableMap = universe.getEditableMapByID(mapID).get
          editableMap.onMouseClicked(event.x, event.y, currentFloor, selectedComponent)
          new UniverseInterface(universe, mapID, currentFloor, selectedComponentID)
        }
      case _ =>
        Future.successful(this)
  end mouseClickOnMap

  def updated: Future[UniverseInterface] =
    Future.successful(UniverseInterface(universe, mapID, currentFloor, selectedComponentID))
end UniverseInterface

object UniverseInterface:
  inline val ComponentIconSize = 30

  final class PaletteGroup(val id: String, val title: String, val components: List[PaletteComponent])

  final class PaletteComponent(val componentID: String, val icon: ImageBitmap)

  final class Map(
    val id: String,
    val floors: Int,
    val currentFloor: Int,
    val currentFloorRect: (Double, Double),
    val floorImage: ImageBitmap
  )

  object Map:
    def buildFromUniverse(universe: Universe, mapID: String, currentFloor: Int): Map =
      val underlying = universe.getEditableMapByID(mapID).get
      val floors = underlying.floors
      val dimensions = underlying.getFloorRect(currentFloor)
      val currentFloorRect = (dimensions.width, dimensions.height)
      val floorImage = underlying.drawFloor(currentFloor)
      Map(mapID, floors, currentFloor, currentFloorRect, floorImage)
    end buildFromUniverse
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
    end convertedEditor

    InspectedObject.InspectedProperty(prop.name, prop.stringRepr, convertedEditor, prop.setStringRepr)
  end convertInspectedProperty
end UniverseInterface
