package com.funlabyrinthe.editor.renderer.inspector

import scala.scalajs.js

import scala.concurrent.ExecutionContext.Implicits.global

import com.raquo.laminar.api.L.{*, given}

import be.doeraene.webcomponents.ui5
import be.doeraene.webcomponents.ui5.configkeys.{IconName, TableMode}

import com.funlabyrinthe.core.graphics.Painter.PainterItem

import com.funlabyrinthe.editor.renderer.{ErrorHandler, UserErrorMessage}
import com.funlabyrinthe.editor.renderer.electron.fileService

import InspectedObject.*

class ObjectInspector(root: Signal[InspectedObject], setPropertyHandler: Observer[PropSetEvent[?]])(using ErrorHandler):
  private def setPropertyHandler2[T] = setPropertyHandler.contramap { (args: (T, InspectedProperty[T])) =>
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

  private def propertyRow(initial: InspectedProperty[?], signal: Signal[InspectedProperty[?]], isSelected: Signal[Boolean]): Element =
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

  private def propertyDisplayCell(signal: Signal[InspectedProperty[?]]): Element =
    span(
      child <-- signal.map(_.valueDisplayString),
    )
  end propertyDisplayCell

  private def propertyEditorCell(signal: Signal[InspectedProperty[?]]): Element =
    div(
      child <-- signal.splitOne(_.editor) { (editor, _, signal: Signal[InspectedProperty[?]]) =>
        editor match
          case editor: PropertyEditor[t] =>
            val signal1 = signal.asInstanceOf[Signal[InspectedProperty[t]]] // they all have the same editor, so this is fine
            editor match
              case PropertyEditor.StringValue                  => stringPropertyEditor(signal1)
              case PropertyEditor.BooleanValue                 => booleanPropertyEditor(signal1)
              case PropertyEditor.IntValue                     => intPropertyEditor(signal1)
              case PropertyEditor.StringChoices(choices)       => stringChoicesPropertyEditor(choices, signal1)
              case PropertyEditor.PainterEditor                => painterPropertyEditor(signal1)
              case PropertyEditor.FiniteSet(availableElements) => finiteSetPropertyEditor(availableElements, signal1)
      },
    )
  end propertyEditorCell

  private def stringPropertyEditor(signal: Signal[InspectedProperty[String]]): Element =
    ui5.Input(
      className := "object-inspector-value-input",
      value <-- signal.map(_.editorValue),
      _.events.onChange.mapToValue.compose(_.withCurrentValueOf(signal)) --> setPropertyHandler2,
    )
  end stringPropertyEditor

  private def booleanPropertyEditor(signal: Signal[InspectedProperty[Boolean]]): Element =
    ui5.Switch(
      className := "object-inspector-value-input",
      _.textOff := "false",
      _.textOn := "true",
      _.checked <-- signal.map(_.editorValue),
      _.events.onCheckedChange.mapToChecked.compose(_.withCurrentValueOf(signal)) --> setPropertyHandler2,
    )
  end booleanPropertyEditor

  private def intPropertyEditor(signal: Signal[InspectedProperty[Int]]): Element =
    ui5.StepInput(
      className := "object-inspector-value-input",
      _.value <-- signal.map(_.editorValue),
      _.events.onChange.map(_.target.value.toInt).compose(_.withCurrentValueOf(signal)) --> setPropertyHandler2,
    )
  end intPropertyEditor

  private def stringChoicesPropertyEditor(choices: List[String], signal: Signal[InspectedProperty[String]]): Element =
    ui5.ComboBox(
      className := "object-inspector-value-input",
      value <-- signal.map(_.editorValue),
      _.events.onChange.mapToValue.compose(_.withCurrentValueOf(signal)) --> setPropertyHandler2,
      choices.map(choice => ui5.ComboBoxItem(_.text := choice)),
    )
  end stringChoicesPropertyEditor

  private def painterPropertyEditor(signal: Signal[InspectedProperty[List[PainterItem]]]): Element =
    div(
      span("(painter)"),
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
                    val newItems: List[PainterItem] = List(PainterItem.ImageDescription(name))
                    setPropertyHandler2.onNext(newItems, prop)
                  case _ =>
                    throw UserErrorMessage(s"Invalid image file: $imageFile")
          }
        },
      ),
      ui5.Button(
        _.icon := IconName.decline,
        _.tooltip := "Clear",
        _.events.onClick.mapTo(Nil).compose(_.withCurrentValueOf(signal)) --> setPropertyHandler2,
      ),
    )
  end painterPropertyEditor

  private def finiteSetPropertyEditor(availableElements: List[String], signal: Signal[InspectedProperty[List[String]]]): Element =
    val selectedSet = signal.map(_.editorValue.toSet).distinct
    ui5.MultiComboBox(
      className := "object-inspector-value-input",
      _.events.onSelectionChange.compose(_.withCurrentValueOf(signal)) --> { (event, prop) =>
        val newEditorValue = event.detail.items.toList.map(_.dataset("elemstring"))
        setPropertyHandler2.onNext((newEditorValue, prop))
      },
      availableElements.map { elem =>
        ui5.MultiComboBox.item(
          _.text := elem,
          dataAttr("elemstring") := elem,
          _.selected <-- selectedSet.map(_.contains(elem)),
        )
      },
    )
  end finiteSetPropertyEditor
end ObjectInspector
