package com.funlabyrinthe.editor.renderer.inspector

import scala.scalajs.js

import scala.concurrent.ExecutionContext.Implicits.global

import com.raquo.laminar.api.L.{*, given}

import be.doeraene.webcomponents.ui5
import be.doeraene.webcomponents.ui5.configkeys.{IconName, TableMode}

import com.funlabyrinthe.editor.renderer.{ErrorHandler, UserErrorMessage}
import com.funlabyrinthe.editor.renderer.electron.fileService

import InspectedObject.*

class ObjectInspector(root: Signal[InspectedObject], setPropertyHandler: Observer[PropSetEvent])(using ErrorHandler):
  private val setPropertyHandler2 = setPropertyHandler.contramap { (args: (String, InspectedProperty)) =>
    PropSetEvent(args._2, args._1)
  }

  lazy val topElement: Element =
    val selected = Var[Option[String]](None)
    ui5.Table(
      width := "100%",
      _.mode := TableMode.SingleSelect,
      _.slots.columns := ui5.Table.column(
        width := "50%",
        "Property",
      ),
      _.slots.columns := ui5.Table.column(
        width := "50px",
        "Value",
      ),
      _.events
        .onSelectionChange
        .filter(e => !js.isUndefined(e.detail.selectedRows)) // work around events coming from nested ComboBoxes
        .map(_.detail.selectedRows.headOption.flatMap(_.dataset.get("propertyname"))) --> selected.writer,
      children <-- root.map(_.properties).split(_.name) { (propName, initial, signal) =>
        val isSelected = selected.signal.map(_.contains(initial.name)).distinct
        propertyRow(initial, signal, isSelected)
      },
    )
  end topElement

  private def propertyRow(initial: InspectedProperty, signal: Signal[InspectedProperty], isSelected: Signal[Boolean]): Element =
    val selected = Var(false)
    ui5.TableRow(
      dataAttr("propertyname") := initial.name,
      _.cell(child <-- signal.map(_.name)),
      _.cell(
        child <-- isSelected.map { selected =>
          if selected then propertyEditorCell(signal)
          else propertyDisplayCell(signal)
        },
      ),
    )
  end propertyRow

  private def propertyDisplayCell(signal: Signal[InspectedProperty]): Element =
    span(
      child <-- signal.map { prop =>
        if prop.editor == PropertyEditor.PainterEditor then "<painter>"
        else prop.stringRepr
      }
    )
  end propertyDisplayCell

  private def propertyEditorCell(signal: Signal[InspectedProperty]): Element =
    div(
      child <-- signal.map(_.editor).distinct.map {
        case PropertyEditor.StringValue            => stringPropertyEditor(signal)
        case PropertyEditor.BooleanValue           => booleanPropertyEditor(signal)
        case PropertyEditor.IntValue               => intPropertyEditor(signal)
        case PropertyEditor.StringChoices(choices) => stringChoicesPropertyEditor(choices, signal)
        case PropertyEditor.PainterEditor          => painterPropertyEditor(signal)
      },
    )
  end propertyEditorCell

  private def stringPropertyEditor(signal: Signal[InspectedProperty]): Element =
    ui5.Input(
      className := "object-inspector-value-input",
      value <-- signal.map(_.stringRepr),
      _.events.onChange.mapToValue.compose(_.withCurrentValueOf(signal)) --> setPropertyHandler2,
    )
  end stringPropertyEditor

  private def booleanPropertyEditor(signal: Signal[InspectedProperty]): Element =
    ui5.Switch(
      className := "object-inspector-value-input",
      _.textOff := "false",
      _.textOn := "true",
      _.checked <-- signal.map(_.stringRepr == "true"),
      _.events.onCheckedChange.mapToChecked.map(_.toString()).compose(_.withCurrentValueOf(signal)) --> setPropertyHandler2,
    )
  end booleanPropertyEditor

  private def intPropertyEditor(signal: Signal[InspectedProperty]): Element =
    ui5.StepInput(
      className := "object-inspector-value-input",
      _.value <-- signal.map(_.stringRepr.toInt),
      _.events.onChange.map(_.target.value.toInt.toString()).compose(_.withCurrentValueOf(signal)) --> setPropertyHandler2,
    )
  end intPropertyEditor

  private def stringChoicesPropertyEditor(choices: List[String], signal: Signal[InspectedProperty]): Element =
    ui5.ComboBox(
      className := "object-inspector-value-input",
      value <-- signal.map(_.stringRepr),
      _.events.onChange.mapToValue.compose(_.withCurrentValueOf(signal)) --> setPropertyHandler2,
      choices.map(choice => ui5.ComboBoxItem(_.text := choice)),
    )
  end stringChoicesPropertyEditor

  private def painterPropertyEditor(signal: Signal[InspectedProperty]): Element =
    div(
      span("<painter>"),
      ui5.Button(
        _.icon := IconName.edit,
        _.tooltip := "Edit",
        _.events.onClick.compose(_.withCurrentValueOf(signal)) --> { (event, prop) =>
          ErrorHandler.handleErrors {
            for
              imageFileOpt <- fileService.showOpenImageDialog().toFuture
            yield
              for imageFile <- imageFileOpt do
                val pathRegExp = raw"""^.*/([^/]+/[^/]+)\.(?:png|gif)$$""".r
                imageFile match
                  case pathRegExp(name) =>
                    setPropertyHandler2.onNext((name, prop))
                  case _ =>
                    throw UserErrorMessage(s"Invalid image file: $imageFile")
          }
        },
      ),
      ui5.Button(
        _.icon := IconName.decline,
        _.tooltip := "Clear",
        _.events.onClick.mapTo("").compose(_.withCurrentValueOf(signal)) --> setPropertyHandler2,
      ),
    )
  end painterPropertyEditor
end ObjectInspector
