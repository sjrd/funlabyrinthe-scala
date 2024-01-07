package com.funlabyrinthe.corebridge

import scala.collection.immutable.TreeSet

import scala.scalajs.js
import scala.scalajs.js.JSConverters.*

import org.scalajs.dom

import com.funlabyrinthe.core
import com.funlabyrinthe.coreinterface as intf
import com.funlabyrinthe.coreinterface.Constants.*
import com.funlabyrinthe.core.graphics.Painter

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
    import core.reflect.InspectedType
    import intf.InspectedObject.*

    val propsData = instance.reflect().reflectProperties(instance)

    propsData.flatMap { propData =>
      var specialStringRepr: Option[String] = None

      val optEditorAndSetter: Option[(PropertyEditor, js.Function1[String, Unit])] = propData.tpe match
        case _ if propData.isReadOnly =>
          None
        case InspectedType.String =>
          Some((PropertyEditor.StringValue(), propData.asWritable.value = _))
        case InspectedType.Boolean =>
          Some((PropertyEditor.BooleanValue(), str => propData.asWritable.value = (str == "true")))
        case InspectedType.Int =>
          Some((PropertyEditor.IntValue(), str => propData.asWritable.value = str.toInt))
        case InspectedType.TreeSetOf(FiniteSetInspectedType(availableValueStrings, stringToValue)) =>
          val oldValue = propData.value.asInstanceOf[TreeSet[Any]]
          specialStringRepr = Some(oldValue.mkString(";"))
          Some((PropertyEditor.FiniteSet(availableValueStrings.toJSArray), { str =>
            val newValue = oldValue.empty ++ str.split(';').map(stringToValue)
            propData.asWritable.value = newValue
          }))
        case FiniteSetInspectedType(valueStrings, stringToValue) =>
          Some((PropertyEditor.StringChoices(valueStrings.toJSArray), { str =>
            propData.asWritable.value = stringToValue(str)
          }))
        case InspectedType.MonoClass(cls) if cls == classOf[Painter] =>
          Some((PropertyEditor.PainterValue(), { str =>
            val names = str.split(";").toList
            val descs = names.map(Painter.PainterItem.ImageDescription(_))
            val newPainter = propData.value.asInstanceOf[Painter].empty ++ descs
            propData.asWritable.value = newPainter
          }))
        case _ =>
          None

      for (editor0, setter0) <- optEditorAndSetter yield
        val stringRepr0 = specialStringRepr.getOrElse(propData.valueString)
        new InspectedProperty {
          val name = propData.name
          val stringRepr: String = stringRepr0
          val editor = editor0
          val setStringRepr = setter0
        }
    }
  end buildInspectedProperties

  private object FiniteSetInspectedType:
    import core.reflect.InspectedType

    def unapply(tpe: InspectedType)(using core.Universe): Option[(List[String], String => Any)] = tpe match
      case InspectedType.EnumClass(values) =>
        Some((values.map(_.toString()), { str =>
          values.find(_.toString() == str).getOrElse {
            throw IllegalArgumentException(
              s"'$str' is not a valid values; possible choices are ${values.mkString(", ")}"
            )
          }
        }))

      case InspectedType.MonoClass(cls) if classOf[core.Component].isAssignableFrom(cls) =>
        cls match
          case cls: Class[a] =>
            val universe = summon[core.Universe]
            val available = universe.allComponents.filter(cls.isInstance(_)).toList.map(_.toString()).sorted
            Some((available, { str =>
              cls.cast(universe.getComponentByID(str))
            }))

      case _ =>
        None
    end unapply
  end FiniteSetInspectedType
end EditableComponent
