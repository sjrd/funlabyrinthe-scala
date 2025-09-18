package com.funlabyrinthe.editor.renderer.inspector

import scala.scalajs.js
import scala.scalajs.js.JSConverters.JSRichOption

import org.scalajs.dom

import com.raquo.laminar.api.L.{*, given}

import be.doeraene.webcomponents.ui5
import be.doeraene.webcomponents.ui5.configkeys.*
import be.doeraene.webcomponents.ui5.eventtypes.{HasDetail, HasColor, MoveEventDetail}

import com.funlabyrinthe.editor.renderer.{ErrorHandler, JSPI, PainterItem, UserErrorMessage}
import com.funlabyrinthe.editor.renderer.electron.fileService
import com.funlabyrinthe.editor.renderer.UIComponents.*

import InspectedObject.*

class ObjectInspector(root: Signal[InspectedObject], setPropertyHandler: Observer[PropSetEvent[?]])(using ErrorHandler):
  import ObjectInspector.*

  private def setPropertyHandler2[T] = setPropertyHandler.contramap { (args: (T, InspectedProperty[T])) =>
    PropSetEvent(args._2, args._1)
  }

  val painterEditorOpenBus = new EventBus[(List[PainterItem], List[PainterItem] => Unit)]

  private val selectedPath = Var[Option[PropertyPath]](None)

  lazy val topElement: Element =
    ui5.Tree(
      width := "100%",
      _.selectionMode := ListMode.Single,
      _.events
        .onSelectionChange
        .filter(e => !js.isUndefined(e.detail.selectedItems)) // work around events coming from nested ComboBoxes
        .map(_.detail.selectedItems.headOption.flatMap(_.propertyPath)) --> selectedPath.writer,
      children <-- allPropertyRows(),
    )
  end topElement

  private def allPropertyRows(): Signal[List[Element]] =
    root.map(_.properties).split(_.name) { (name, initial, signal) =>
      propertyRow(name :: Nil, initial, signal)
    }
  end allPropertyRows

  private def propertyRow(propertyPath: PropertyPath, initial: InspectedProperty[?], signal: Signal[InspectedProperty[?]]): Element =
    val isSelected = selectedPath.signal.map(_.contains(propertyPath)).distinct

    def propertyPathString(path: PropertyPath): String = path match
      case root :: Nil            => root.toString()
      case (elem: Int) :: rest    => s"${propertyPathString(rest)}($elem)"
      case (elem: String) :: rest => s"${propertyPathString(rest)}.$elem"
      case Nil                    => "" // for exhaustivity

    ui5.TreeItemCustom(
      Setter(thisNode => thisNode.ref.propertyPath = Some(propertyPath)),
      cls := "funlaby-inspector-treeitem",
      _.hasChildren <-- signal.map(_.editor.hasChildren),
      _.slots.content := div(
        cls := "funlaby-inspector-row",
        div(
          cls := "funlaby-inspector-cell",
          cls := "funlaby-inspector-propname",
          title := propertyPathString(propertyPath),
          propertyPath.head.toString(),
          child.maybe <-- signal.combineWith(isSelected).map { (prop, selected) =>
            prop.remove.filter(_ => selected).map { remove =>
              ui5.Button(
                _.icon := IconName.delete,
                _.design := ButtonDesign.Transparent,
                _.events.onClick --> { e => remove() },
              )
            }
          },
        ),
        child <-- isSelected.map { selected =>
          if selected then propertyEditorCell(signal)
          else propertyDisplayCell(signal)
        },
      ),
      children <-- signal.splitOne(_.editor) { (editor, _, signal: Signal[InspectedProperty[?]]) =>
        editor match
          case editor: PropertyEditor[t] =>
            val signal1 = signal.asInstanceOf[Signal[InspectedProperty[t]]] // they all have the same editor, so this is fine
            editor match
              case PropertyEditor.ItemList(elemEditor) => itemListChildren(propertyPath, elemEditor, signal1)
              case _                                   => Signal.fromValue(Nil)
      }.flattenSwitch,
    )
  end propertyRow

  private def itemListChildren[E](
    propertyPath: PropertyPath,
    elemEditor: PropertyEditor[E],
    signal: Signal[InspectedProperty[List[E]]]
  ): Signal[List[Element]] =
    def addItem(prop: InspectedProperty[List[E]]): Unit =
      val prevValues = prop.editorValue
      val newValues = prevValues :+ comeUpWithDefaultValue(elemEditor)
      setPropertyHandler2.onNext(newValues, prop)
      selectedPath.set(Some(prevValues.size :: propertyPath))
    end addItem

    val addItemRow =
      val addItemPath = "new" :: propertyPath
      val isSelected = selectedPath.signal.map(_.contains(addItemPath)).distinct

      ui5.TreeItemCustom(
        Setter(thisNode => thisNode.ref.propertyPath = Some(addItemPath)),
        cls := "funlaby-inspector-treeitem",
        _.slots.content := div(
          cls := "funlaby-inspector-row",
          div(
            cls := "funlaby-inspector-cell",
            cls := "funlaby-inspector-propname",
          ),
          div(
            cls := "funlaby-inspector-cell",
            cls := "funlaby-inspector-value",
            ui5.Button(
              title := "Add an item",
              _.icon := IconName.add,
              _.design := ButtonDesign.Positive,
              _.events.onClick.compose(_.sample(signal)) --> { prop =>
                ErrorHandler.handleErrors {
                  addItem(prop)
                }
              },
            ),
          ),
        ),
      )
    end addItemRow

    signal
      .map { prop =>
        prop.editorValue.zipWithIndex.map { (elemValue, index) =>
          new InspectedProperty[E](
            index.toString(),
            elemValue.toString(),
            elemEditor,
            elemValue,
            { (newValue: E) =>
              prop.setEditorValue(prop.editorValue.updated(index, newValue))
            },
            Some({ () =>
              val (before, after) = prop.editorValue.splitAt(index)
              setPropertyHandler2.onNext(before ::: after.drop(1), prop)
              selectedPath.set(Some(propertyPath))
            }),
          )
        }
      }
      .splitByIndex { (index, initialElem, elemSignal) =>
        propertyRow(index :: propertyPath, initialElem, elemSignal)
      }
      .map(_ :+ addItemRow)
  end itemListChildren

  private def comeUpWithDefaultValue[T](editor: PropertyEditor[T]): T =
    def fail(): Nothing =
      throw UserErrorMessage("There is no possible value to add to this list")

    editor match
      case PropertyEditor.StringValue                  => ""
      case PropertyEditor.BooleanValue                 => false
      case PropertyEditor.IntValue                     => 0
      case PropertyEditor.StringChoices(choices)       => choices.headOption.getOrElse(fail())
      case PropertyEditor.ItemList(elemEditor)         => Nil
      case PropertyEditor.PainterEditor                => Nil
      case PropertyEditor.ColorEditor                  => 0xff // opaque black
      case PropertyEditor.FiniteSet(availableElements) => Nil
  end comeUpWithDefaultValue

  private def propertyDisplayCell(signal: Signal[InspectedProperty[?]]): HtmlElement =
    div(
      cls := "funlaby-inspector-cell",
      cls := "funlaby-inspector-value",
      title <-- signal.map(_.valueDisplayString),
      child <-- signal.map(_.valueDisplayString),
    )
  end propertyDisplayCell

  private def propertyEditorCell(signal: Signal[InspectedProperty[?]]): HtmlElement =
    div(
      cls := "funlaby-inspector-cell",
      cls := "funlaby-inspector-value",
      child <-- signal.splitOne(_.editor) { (editor, _, signal: Signal[InspectedProperty[?]]) =>
        editor match
          case editor: PropertyEditor[t] =>
            val signal1 = signal.asInstanceOf[Signal[InspectedProperty[t]]] // they all have the same editor, so this is fine
            editor match
              case PropertyEditor.StringValue                  => stringPropertyEditor(signal1)
              case PropertyEditor.BooleanValue                 => booleanPropertyEditor(signal1)
              case PropertyEditor.IntValue                     => intPropertyEditor(signal1)
              case PropertyEditor.StringChoices(choices)       => stringChoicesPropertyEditor(choices, signal1)
              case PropertyEditor.ItemList(elemEditor)         => itemListPropertyEditor(elemEditor, signal1)
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

  private def itemListPropertyEditor[E](elemEditor: PropertyEditor[E], signal: Signal[InspectedProperty[List[E]]]): Element =
    // There is no edit control here; lists are edited through their children elements
    div(
      span(child.text <-- signal.map(_.valueDisplayString)),
    )
  end itemListPropertyEditor

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

  lazy val painterEditorDialog: Element =
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
      _.stretch := true,
      _.headerText := "Painter editor",
      sectionTag(
        styleAttr := "height: 100%",
        twoColumns(
          painterItemsEditList(painterItems),
          imageDirectoryExplorer(selectImageObserver),
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
    val pathVar = Var[List[String]](Nil)

    val listingSignal = pathVar.signal.flatMapSwitch { path =>
      Signal.fromJsPromise(fileService.listImageDirectory(path.mkString("/", "/", ""))).map(path -> _)
    }
    val dirListing = listingSignal.map((path, listing) => (path, listing.fold(Nil)(_.subdirectories.toList)))
    val imageListing = listingSignal.map((path, listing) => (path, listing.fold(Nil)(_.images.toList)))

    ui5.UList(
      _.slots.header := ui5.Breadcrumbs(
        _.design := BreadcrumbsDesign.NoCurrentPage,
        children <-- pathVar.signal.map { path =>
          path.inits.toList.reverse.map { targetPath =>
            ui5.BreadcrumbsItem(
              targetPath.lastOption.getOrElse("Images"),
              dataAttr("targetpath") := targetPath.mkString("/"),
            )
          }
        },
        _.events.onItemClick.preventDefault --> { event =>
          for targetPathStr <- event.detail.item.dataset.get("targetpath") do
            pathVar.set(if targetPathStr == "" then Nil else targetPathStr.split('/').toList)
        },
      ),
      _.loading <-- listingSignal.map(_._2.isEmpty),
      children <-- dirListing.map { (path, subdirs) =>
        subdirs.map { subdir =>
          ui5.UList.item(
            subdir,
            _.slots.image := ui5.Icon(
              _.name := IconName.folder,
              width := "30px",
              height := "30px",
              padding := "5px",
            ),
            onClick.stopPropagation --> { e =>
              pathVar.set(path :+ subdir)
            },
          )
        }
      },
      children <-- imageListing.map { (path, imageEntries) =>
        imageEntries.map { imageEntry =>
          val imagePath = (path :+ imageEntry.name).mkString("/")
          ui5.UList.item(
            imageEntry.name,
            _.slots.image := img(
              src := "./Resources/Images/" + imagePath,
              width := "30px",
              height := "30px",
              padding := "5px",
            ),
            onClick.stopPropagation.mapTo(imagePath.stripSuffix(".png")) --> selectImageObserver,
          )
        }
      },
    )
  end imageDirectoryExplorer
end ObjectInspector

object ObjectInspector:
  private type PropertyPath = List[String | Int]

  private trait WithPropertyPath extends js.Object:
    var propertyPath: js.UndefOr[PropertyPath]

  extension (self: dom.Element)
    private def propertyPath: Option[PropertyPath] =
      self.asInstanceOf[WithPropertyPath].propertyPath.toOption

    private def propertyPath_=(v: Option[PropertyPath]): Unit =
      self.asInstanceOf[WithPropertyPath].propertyPath = v.orUndefined
  end extension
end ObjectInspector
