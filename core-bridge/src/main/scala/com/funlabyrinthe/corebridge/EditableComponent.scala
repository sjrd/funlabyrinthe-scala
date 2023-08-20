package com.funlabyrinthe.corebridge

import scala.scalajs.js
import scala.scalajs.js.JSConverters.*

import org.scalajs.dom

import com.funlabyrinthe.core
import com.funlabyrinthe.coreinterface as intf
import com.funlabyrinthe.coreinterface.Constants.*

import com.funlabyrinthe.graphics.html.GraphicsContextWrapper

final class EditableComponent(universe: Universe, val underlying: core.Component) extends intf.EditableComponent:
  import EditableComponent.*

  def id: String = underlying.id

  def category: intf.ComponentCategory = new {
    val id = underlying.category.id
    val name = underlying.category.text
  }

  def drawIcon(): dom.ImageBitmap =
    val canvas = new dom.OffscreenCanvas(ComponentIconSize, ComponentIconSize)
    val gc = canvas.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]
    val drawContext = new core.graphics.DrawContext(new GraphicsContextWrapper(gc),
        new core.graphics.Rectangle2D(0, 0, ComponentIconSize, ComponentIconSize))
    underlying.drawIcon(drawContext)
    canvas.transferToImageBitmap()
  end drawIcon

  def inspect(): intf.InspectedObject =
    new intf.InspectedObject {
      val properties = buildInspectedProperties(underlying).toJSArray
    }
  end inspect
end EditableComponent

object EditableComponent:
  private def buildInspectedProperties(instance: core.reflect.Reflectable): List[intf.InspectedObject.InspectedProperty] =
    import core.reflect.InspectedType
    import intf.InspectedObject.*

    val propsData = instance.reflect().reflectProperties(instance)

    propsData.flatMap { propData =>
      val optEditorAndSetter: Option[(PropertyEditor, js.Function1[String, Unit])] = propData.tpe match
        case _ if propData.isReadOnly =>
          None
        case InspectedType.String =>
          Some((PropertyEditor.StringValue(), propData.asWritable.value = _))
        case InspectedType.Boolean =>
          Some((PropertyEditor.BooleanValue(), str => propData.asWritable.value = (str == "true")))
        case InspectedType.EnumClass(values) =>
          Some((PropertyEditor.StringChoices(values.map(_.toString()).toJSArray), { str =>
            propData.asWritable.value = values.find(_.toString() == str).getOrElse {
              throw IllegalArgumentException(
                s"'$str' is not a valid values; possible choices are ${values.mkString(", ")}"
              )
            }
          }))
        case _ =>
          None

      for (editor0, setter0) <- optEditorAndSetter yield
        new InspectedProperty {
          val name = propData.name
          val stringRepr: String = propData.valueString
          val editor = editor0
          val setStringRepr = setter0
        }
    }
  end buildInspectedProperties
end EditableComponent
