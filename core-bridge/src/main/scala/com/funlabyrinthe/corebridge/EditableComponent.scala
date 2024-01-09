package com.funlabyrinthe.corebridge

import scala.collection.immutable.TreeSet

import scala.scalajs.js
import scala.scalajs.js.JSConverters.*

import org.scalajs.dom

import com.funlabyrinthe.core
import com.funlabyrinthe.coreinterface as intf
import com.funlabyrinthe.coreinterface.Constants.*
import com.funlabyrinthe.core.graphics.Painter
import com.funlabyrinthe.core.inspecting.*

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

  def isComponentCreator: Boolean =
    underlying.isInstanceOf[core.ComponentCreator]

  def createNewComponent(): intf.EditableComponent =
    underlying match
      case underlying: core.ComponentCreator =>
        val createdComponent = underlying.createNewComponent()
        universe.getEditableComponent(createdComponent)
      case _ =>
        throw UnsupportedOperationException(s"$this is not a component creator and cannot create components")
  end createNewComponent

  def inspect(): intf.InspectedObject =
    new intf.InspectedObject {
      val properties = buildInspectedProperties(underlying)(using underlying.universe).toJSArray
    }
  end inspect
end EditableComponent

object EditableComponent:
  private def buildInspectedProperties(instance: core.reflect.Reflectable)(using core.Universe): List[intf.InspectedObject.InspectedProperty] =
    import intf.InspectedObject.*

    val propsDataOrig = instance.reflect().reflectProperties(instance)
    val propsDataOfAttributes = instance match
      case instance: core.CorePlayer    => instance.attributes.reflect().reflectProperties(instance.attributes)
      case instance: core.ReifiedPlayer => instance.attributes.reflect().reflectProperties(instance.attributes)
      case _                            => Nil
    val propsData = propsDataOrig ::: propsDataOfAttributes

    for
      propData <- propsData
      inspectable <- propData.inspectable
    yield
      val propName = propData.name
      val value = propData.value
      val editor = inspectable.editor

      val display: String = inspectable.display(value)

      def build[EditorValueType](
        editor: (Editor { type ValueType = inspectable.EditorValueType }) & (Editor { type ValueType = EditorValueType }),
        propertyEditor: PropertyEditor,
        serialize: EditorValueType => String,
        deserialize: String => EditorValueType,
      ): InspectedProperty =
        val editorValue: editor.ValueType = inspectable.toEditorValue(value)
        val stringRepr0 = serialize(editorValue)
        val setter0: js.Function1[String, Unit] = { strValue =>
          val newEditorValue: editor.ValueType = deserialize(strValue)
          val newValue = inspectable.fromEditorValue(newEditorValue)
          propData.asWritable.value = newValue
        }
        new InspectedProperty {
          val name = propName
          val stringRepr = stringRepr0
          val editor = propertyEditor
          val setStringRepr = setter0
        }
      end build

      editor match
        case editor: Editor.Text.type =>
          build[String](
            editor,
            PropertyEditor.StringValue(),
            identity,
            identity,
          )

        case editor: Editor.Switch.type =>
          build[Boolean](
            editor,
            PropertyEditor.BooleanValue(),
            _.toString(),
            _ == "true",
          )

        case editor @ Editor.SmallInteger(minValue, maxValue, step) =>
          build[Int](
            editor,
            PropertyEditor.IntValue(),
            _.toString(),
            _.toInt,
          )

        case editor @ Editor.StringChoices(choices) =>
          build[String](
            editor,
            PropertyEditor.StringChoices(choices.toJSArray),
            identity,
            identity,
          )

        case editor @ Editor.MultiStringChoices(choices) =>
          build[List[String]](
            editor,
            PropertyEditor.FiniteSet(choices.toJSArray),
            _.mkString(";"),
            _.split(';').toList,
          )

        case editor: Editor.Painter.type =>
          build[List[Painter.PainterItem]](
            editor,
            PropertyEditor.PainterValue(),
            items => items.map(_.toString()).mkString(";"),
            strValue => strValue.split(';').toList.map(Painter.PainterItem.ImageDescription(_)),
          )
  end buildInspectedProperties
end EditableComponent
