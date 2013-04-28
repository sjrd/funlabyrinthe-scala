package com.funlabyrinthe.editor.inspector.jfx

import com.funlabyrinthe.editor.inspector._

import scala.reflect.runtime.universe._

import scala.collection.mutable

import javafx.scene.input.{ KeyEvent => jfxKeyEvent }
import javafx.scene.input.KeyCode._

import scalafx.Includes._
import scalafx.scene.layout._
import scalafx.scene.control._
import scalafx.scene.input._
import scalafx.scene.shape._
import scalafx.geometry._

import scalafx.beans.property._
import scalafx.beans.value.ObservableValue
import scalafx.collections.ObservableBuffer

class Inspector extends ScrollPane {
  val inspector = new com.funlabyrinthe.editor.inspector.Inspector

  hbarPolicy = ScrollPane.ScrollBarPolicy.NEVER
  fitToWidth = true

  private val _inspectedObject = ObjectProperty[Option[AnyRef]](None)
  def inspectedObject = _inspectedObject
  def inspectedObject_=(v: Option[AnyRef]) {
    inspectedObject() = v
  }

  inspectedObject onChange {
    (_, _, instance) =>
      inspector.inspectedObject = instance
  }

  class Descriptor(val editor: Editor, val level: Int) {
    var expanded: Boolean = false
  }
  val descriptors = new ObservableBuffer[Descriptor]

  inspector.onChange = {
    descriptors.clear()
    descriptors ++= inspector.descriptors map (new Descriptor(_, 0))
  }

  private val propertiesTable = new TableView(descriptors) {
    editable = true

    columns += new TableColumn[Descriptor, Descriptor] {
      text = "Properties"
      sortable = false
      editable = false
      cellValueFactory = {
        features =>
          val descriptor = features.value
          ReadOnlyObjectWrapper(descriptor).readOnlyProperty
      }
      cellFactory = { column =>
        new PropertyNameTableCell
      }
    }.delegate

    columns += new TableColumn[Descriptor, Descriptor] {
      text = "Values"
      sortable = false
      editable = true
      cellValueFactory = {
        features =>
          val descriptor = features.value
          ReadOnlyObjectWrapper(descriptor).readOnlyProperty
      }
      cellFactory = { column =>
        new PropertyValueTableCell
      }
    }.delegate
  }

  content = propertiesTable

  class PropertyNameTableCell extends javafx.scene.control.TableCell[Descriptor, Descriptor] {
    val wrapper: TableCell[Descriptor, Descriptor] = this
    import wrapper._

    def editor: Editor = getItem.editor

    val indentRect = new Rectangle {
      width = 1
      height = 1
    }
    val expandButton = new Button {
      text = "+"
      onAction = expandCollapse()
    }
    val label = new Label {
      text = ""
    }
    val content = new HBox {
      spacing = 4.0
      content = List(indentRect, expandButton, label)
    }

    private def expandCollapse() {
      val descriptor = getItem
      val editor = descriptor.editor

      if (editor.hasChildren) {
        descriptor.expanded = !descriptor.expanded
        val level = descriptor.level
        val index = descriptors.indexOf(descriptor)
        assert(index >= 0)

        if (descriptor.expanded) {
          val childLevel = level+1
          val children = editor.children map (new Descriptor(_, childLevel))
          descriptors.insertAll(index+1, children)
        } else {
          val endIndex0 = descriptors.indexWhere(_.level <= level, index+1)
          val endIndex =
            if (endIndex0 >= 0) endIndex0
            else descriptors.size
          descriptors.removeRange(index+1, endIndex)
        }

        updateItem(getItem, false)
      }
    }

    override def updateItem(item: Descriptor, empty: Boolean) {
      super.updateItem(item, empty)

      if (empty) {
        setText(null)
        setGraphic(null)
      } else {
        indentRect.width = 16*item.level
        expandButton.text =
          if (!item.editor.hasChildren) ""
          else if (item.expanded) "-"
          else "+"
        label.text = item.editor.name

        setGraphic(content)
      }
    }
  }

  class PropertyValueTableCell extends javafx.scene.control.TableCell[Descriptor, Descriptor] {
    val wrapper: TableCell[Descriptor, Descriptor] = this
    import wrapper._

    def editor: Editor = getItem.editor

    val textField: TextField = new TextField {
      this.onKeyReleased = { (e: scalafx.event.Event) =>
        val event = e.delegate.asInstanceOf[jfxKeyEvent]
        event.getCode match {
          case ENTER =>
            if (editor.isStringEditable) {
              editor.valueString = textField.text.value
              commitEdit(getItem)
            } else {
              cancelEdit()
            }

          case ESCAPE =>
            cancelEdit()

          case _ => ()
        }
      }
    }

    override def updateItem(item: Descriptor, empty: Boolean) {
      super.updateItem(item, empty)

      if (empty) {
        setText(null)
        setGraphic(null)
      } else {
        if (isEditing) {
          textField.text = item.editor.valueString
          setText(null)
          setGraphic(textField)
        } else {
          setText(item.editor.valueString)
          setGraphic(null)
        }
      }
    }

    override def startEdit() {
      super.startEdit()

      textField.text = editor.valueString
      textField.editable = editor.isStringEditable

      setText(null)
      setGraphic(textField)

      textField.requestFocus
      textField.selectAll()
    }

    override def cancelEdit() {
      super.cancelEdit()
      setText(editor.valueString)
      setGraphic(null)
    }
  }
}
