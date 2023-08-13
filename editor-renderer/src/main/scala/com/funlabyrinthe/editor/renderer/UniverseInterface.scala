package com.funlabyrinthe.editor.renderer

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import com.funlabyrinthe.core.*
import com.funlabyrinthe.core.graphics.*
import com.funlabyrinthe.core.input.*
import com.funlabyrinthe.core.reflect.*
import com.funlabyrinthe.graphics.html.GraphicsContextWrapper

import org.scalajs.dom.CanvasRenderingContext2D
import org.scalajs.dom.ImageBitmap
import org.scalajs.dom.OffscreenCanvas

import com.funlabyrinthe.editor.renderer.inspector.*

final class UniverseInterface(
  universe: Universe,
  mapID: String,
  currentFloor: Int,
  val selectedComponentID: Option[String],
):
  import UniverseInterface.*

  val paletteComponents: List[PaletteGroup] =
    val groups1 = universe.allComponents.groupMap(_.category) { component =>
      PaletteComponent(component.id, drawComponentIcon(universe, component))
    }
    val groups2 =
      for (category, paletteComponents) <- groups1 yield
        PaletteGroup(category.id, category.text, paletteComponents.toList)
    groups2.toList
  end paletteComponents

  val map = Map.buildFromUniverse(universe, mapID, currentFloor)

  val selectedComponentInspected: InspectedObject =
    buildInspectedObject(universe, selectedComponentID)

  def withSelectedComponentID(selected: Option[String]): UniverseInterface =
    new UniverseInterface(universe, mapID, currentFloor, selected)

  def mouseClickOnMap(event: MouseEvent): Future[UniverseInterface] =
    selectedComponentID match
      case Some(selectedID) =>
        Future {
          val editInterface = universe.getComponentByID(mapID).asInstanceOf[EditableMap].getEditInterface()
          val selectedComponent = universe.getComponentByID(selectedID)
          editInterface.onMouseClicked(event, currentFloor, selectedComponent)
          new UniverseInterface(universe, mapID, currentFloor, selectedComponentID)
        }
      case None =>
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
    val currentFloorRect: Rectangle2D,
    val floorImage: ImageBitmap
  )

  object Map:
    def buildFromUniverse(universe: Universe, mapID: String, currentFloor: Int): Map =
      val underlying = universe.getComponentByID(mapID).asInstanceOf[EditableMap].getEditInterface()
      val floors = underlying.floors
      val currentFloorRect = underlying.getFloorRect(currentFloor)
      val floorImage = drawFloor(underlying, currentFloor, currentFloorRect)
      Map(mapID, floors, currentFloor, currentFloorRect, floorImage)
    end buildFromUniverse

    private def drawFloor(mapInterface: MapEditInterface, floor: Int, floorRect: Rectangle2D): ImageBitmap =
      val canvas = new OffscreenCanvas(floorRect.width, floorRect.height)
      val gc = canvas.getContext("2d").asInstanceOf[CanvasRenderingContext2D]
      val context = new DrawContext(new GraphicsContextWrapper(gc), floorRect)
      mapInterface.drawFloor(context, floor)
      canvas.transferToImageBitmap()
    end drawFloor
  end Map

  private def drawComponentIcon(universe: Universe, component: Component): ImageBitmap =
    val canvas = new OffscreenCanvas(ComponentIconSize, ComponentIconSize)
    val gc = canvas.getContext("2d").asInstanceOf[CanvasRenderingContext2D]
    val drawContext = new DrawContext(new GraphicsContextWrapper(gc),
        new Rectangle2D(0, 0, ComponentIconSize, ComponentIconSize))
    component.drawIcon(drawContext)
    canvas.transferToImageBitmap()
  end drawComponentIcon

  private def buildInspectedObject(universe: Universe, componentID: Option[String]): InspectedObject =
    import InspectedObject.*

    componentID.flatMap(universe.getComponentByIDOption(_)) match
      case None =>
        InspectedObject(Nil)

      case Some(root) =>
        InspectedObject(buildInspectedProperties(root))
  end buildInspectedObject

  private def buildInspectedProperties(instance: Reflectable): List[InspectedObject.InspectedProperty] =
    import InspectedObject.*

    val propsData = instance.reflect().reflectProperties(instance)

    propsData.flatMap { propData =>
      val optEditorAndSetter: Option[(PropertyEditor, String => Unit)] = propData.tpe match
        case _ if propData.isReadOnly =>
          None
        case InspectedType.String =>
          Some((PropertyEditor.StringValue, propData.asWritable.value = _))
        case InspectedType.Boolean =>
          Some((PropertyEditor.BooleanValue, str => {println(s"h $str"); propData.asWritable.value = (str == "true") }))
        case _ =>
          None

      for (editor, setter) <- optEditorAndSetter yield
        InspectedProperty(propData.name, propData.valueString, editor, setter)
    }
  end buildInspectedProperties
end UniverseInterface
