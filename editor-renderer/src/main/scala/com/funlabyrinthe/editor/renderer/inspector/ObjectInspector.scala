package com.funlabyrinthe.editor.renderer.inspector

import scala.scalajs.js

import org.scalajs.dom

import com.raquo.laminar.api.L.{*, given}

import be.doeraene.webcomponents.ui5
import be.doeraene.webcomponents.ui5.configkeys.*
import be.doeraene.webcomponents.ui5.eventtypes.{HasDetail, HasColor, MoveEventDetail}

import com.funlabyrinthe.editor.renderer.{ErrorHandler, JSPI, PainterItem, UserErrorMessage}
import com.funlabyrinthe.editor.renderer.electron.fileService

import InspectedObject.*

class ObjectInspector(root: Signal[InspectedObject], setPropertyHandler: Observer[PropSetEvent[?]])(using ErrorHandler):
  private def setPropertyHandler2[T] = setPropertyHandler.contramap { (args: (T, InspectedProperty[T])) =>
    PropSetEvent(args._2, args._1)
  }

  val painterEditorOpenBus = new EventBus[(List[PainterItem], List[PainterItem] => Unit)]

  lazy val topElement: Element =
    val selected = Var[Option[String]](None)
    ui5.compat.Table(
      width := "100%",
      painterEditorDialog,
      _.mode := TableMode.SingleSelect,
      _.slots.columns := ui5.compat.Table.column(
        width := "50%",
        "Property",
      ),
      _.slots.columns := ui5.compat.Table.column(
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
    def shortName(name: String): String = name.substring(name.lastIndexOf(':') + 1)

    val selected = Var(false)
    ui5.compat.TableRow(
      dataAttr("propertyname") := initial.name,
      _.cell(
        title <-- signal.map(_.name),
        child <-- signal.map(prop => shortName(prop.name)),
      ),
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
              case PropertyEditor.ColorEditor                  => colorPropertyEditor(signal1)
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
          painterEditorOpenBus.emit((prop.editorValue, { newPainterItems =>
            setPropertyHandler2.onNext(newPainterItems, prop)
          }))
          //ErrorHandler.handleErrors {
          //  val imageFileOpt = JSPI.await(fileService.showOpenImageDialog())
          //  for imageFile <- imageFileOpt do
          //    val pathRegExp = raw"""^.*/([^/]+/[^/]+)\.(?:png|gif)$$""".r
          //    imageFile match
          //      case pathRegExp(name) =>
          //        val newItems: List[PainterItem] = List(PainterItem.ImageDescription(name))
          //        setPropertyHandler2.onNext(newItems, prop)
          //      case _ =>
          //        throw UserErrorMessage(s"Invalid image file: $imageFile")
          //}
        },
      ),
      ui5.Button(
        _.icon := IconName.decline,
        _.tooltip := "Clear",
        _.events.onClick.mapTo(Nil).compose(_.withCurrentValueOf(signal)) --> setPropertyHandler2,
      ),
    )
  end painterPropertyEditor

  private def colorPropertyEditor(signal: Signal[InspectedProperty[Int]]): Element =
    import ui5.scaladsl.colour.*

    def packedToCSS(packed: Int): String =
      "#%08x".format(packed)

    def cssToPacked(css: String): Int =
      val rgba = Colour.fromString(css).asRGBAColour
      (rgba.red << 24) | (rgba.green << 16) | (rgba.blue << 8) | (rgba.alpha * 255).toInt

    val openPopoverBus = new EventBus[dom.HTMLElement]
    div(
      span(color <-- signal.map(prop => packedToCSS(prop.editorValue)), "◼"),
      span(child.text <-- signal.map(_.valueDisplayString)),
      ui5.Button(
        idAttr := "color-property-editor-palette-button",
        _.icon := IconName.edit,
        _.tooltip := "Choose a color",
        _.events.onClick.mapToEvent.map(_.target) --> openPopoverBus.writer,
      ),
      ui5.ColourPalettePopover(
        _.showAtOpenerIdFromEvents(openPopoverBus.events.map(_.id)),
        _.showRecentColours := true,
        _.showMoreColours := true,
        _.events.onItemClick.mapToEvent.map(ev => cssToPacked(ev.detail.color)).compose(_.withCurrentValueOf(signal)) --> setPropertyHandler2,
        Colour.someColours.toList.map(color => ui5.ColourPaletteItem(_.value := color)),
      ),
    )
  end colorPropertyEditor

  private lazy val PaletteColors: List[ui5.scaladsl.colour.Colour] =
    import ui5.scaladsl.colour.Colour
    Colour.black :: Colour.white :: Colour.someColours.toList

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

  private lazy val painterEditorDialog: Element =
    val painterEditorOpenSignal: Signal[(List[PainterItem], List[PainterItem] => Unit)] =
      painterEditorOpenBus.events.toSignal((Nil, _ => ()), false)
    val validateFunctionSig = painterEditorOpenSignal.map(_._2)

    val painterItems = Var[List[PainterItem]](Nil)
    val closeEventBus = new EventBus[Unit]

    val selectImageObserver: Observer[String] =
      painterItems.updater { (prev, selectedImage) =>
        prev :+ PainterItem.ImageDescription(selectedImage)
      }

    ui5.Dialog(
      painterEditorOpenSignal.map(_._1) --> painterItems.writer,
      _.showFromEvents(painterEditorOpenBus.events.mapToUnit),
      _.closeFromEvents(closeEventBus.events),
      _.headerText := "Painter editor",
      sectionTag(
        ui5.DynamicSideContent(
          _.equalSplit := true,
          painterItemsEditList(painterItems),
          _.slots.sideContent := Seq(imageDirectoryExplorer(selectImageObserver)),
        )
      ),
      _.slots.footer := div(
        div(flex := "1"),
        ui5.Button(
          _.design := ButtonDesign.Emphasized,
          "Validate",
          _.events.onClick.compose(_.sample(painterItems.signal, validateFunctionSig)) --> { (finalPainterItems, validateFun) =>
            ErrorHandler.handleErrors {
              closeEventBus.emit(())
              validateFun(finalPainterItems)
            }
          },
        ),
        ui5.Button(
          _.design := ButtonDesign.Negative,
          "Cancel",
          _.events.onClick.mapToUnit --> closeEventBus.writer,
        ),
      )
    )
  end painterEditorDialog

  private def painterItemsEditList(
    painterItems: Var[List[PainterItem]],
  ): Element =
    val moveBus: EventBus[MoveEventDetail[ui5.UList.item.Ref]] = new EventBus
    val moveHandler = moveBus.events.withCurrentValueOf(painterItems.signal).map { (eventDetail, items) =>
      val sourceIndex = eventDetail.source.element.dataset("index").toInt
      val destinationIndexShift = if eventDetail.destination.placement == "Before" then 0 else 1
      val destinationIndex = eventDetail.destination.element.dataset("index").toInt + destinationIndexShift

      val itemsSourceRemoved = items.patch(sourceIndex, Nil, 1)
      val itemsAfterChange =
        if destinationIndex < sourceIndex then itemsSourceRemoved.patch(destinationIndex, List(items(sourceIndex)), 0)
        else itemsSourceRemoved.patch(destinationIndex - 1, List(items(sourceIndex)), 0)
      itemsAfterChange
    }

    ui5.UList(
      _.noDataText := "(empty)",
      _.selectionMode := ListMode.Delete,
      children <-- painterItems.signal.map(_.zipWithIndex).split(identity) { (_, painterItemAndIndex, _) =>
        val (painterItem, index) = painterItemAndIndex
        ui5.UList.item(
          painterItem match {
            case PainterItem.ImageDescription(imageName) => imageName
          },
          _.movable := true,
          dataAttr("index") := index.toString(),
          _.slots.image := (painterItem match {
            case PainterItem.ImageDescription(imageName) =>
              img(
                src := "./Resources/Images/" + imageName + ".png",
                width := "30px",
                height := "30px",
                padding := "5px",
                dataAttr("index") := index.toString(),
              )
          }),
          _.slots.deleteButton := ui5.Button(
            _.design := ButtonDesign.Transparent,
            _.icon := IconName.delete,
            onClick.mapTo(index) --> painterItems.updater { (prev, indexToRemove) =>
              val (before, after) = prev.splitAt(index)
              before ::: after.drop(1)
            },
          ),
        )
      },
      _.events.onMoveOver.preventDefault --> Observer.empty,
      _.events.onMove.map(_.detail) --> moveBus.writer,
      moveHandler --> painterItems.writer,
    )
  end painterItemsEditList

  private def imageDirectoryExplorer(selectImageObserver: Observer[String]): HtmlElement =
    val currentDir = Var[String]("/")

    val listingSignal = currentDir.signal.flatMapSwitch { dir =>
      Signal.fromJsPromise(fileService.listImageDirectory(dir))
    }
    val dirListing = listingSignal.map(_.fold(Nil)(_.subdirectories.toList))
    val imageListing = listingSignal.map(_.fold(Nil)(_.images.toList))

    ui5.UList(
      _.headerText <-- currentDir.signal,
      _.loading <-- listingSignal.map(_.isEmpty),
      children <-- dirListing.map { subdirs =>
        subdirs.map { subdir =>
          ui5.UList.item(
            subdir,
            _.slots.image := ui5.Icon(
              _.name := IconName.folder,
            ),
          )
        }
      },
      children <-- currentDir.signal.combineWith(imageListing).map { (curDir, imageEntries) =>
        imageEntries.map { imageEntry =>
          ui5.UList.item(
            imageEntry.name,
            _.slots.image := img(
              src := "./Resources/Images/" + imageEntry.name,
              width := "30px",
              height := "30px",
              padding := "5px",
            ),
          )
        }
      },
    )
  end imageDirectoryExplorer
end ObjectInspector
