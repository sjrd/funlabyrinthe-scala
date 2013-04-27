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

  val descriptors = new ObservableBuffer[Editor]

  inspector.onChange = {
    descriptors.clear()
    descriptors ++= inspector.descriptors
  }

  private val propertiesTable = new TableView(descriptors) {
    editable = true

    columns += new TableColumn[Editor, String] {
      text = "Properties"
      sortable = false
      editable = false
      cellValueFactory = {
        features =>
          val descriptor = features.value
          ReadOnlyStringWrapper(descriptor.name).readOnlyProperty
      }
    }.delegate

    columns += new TableColumn[Editor, Editor] {
      text = "Values"
      sortable = false
      editable = true
      cellValueFactory = {
        features =>
          val descriptor = features.value
          ReadOnlyObjectWrapper(descriptor).readOnlyProperty
      }
      cellFactory = { column =>
        new EditorTableCell
      }
    }.delegate
  }

  content = propertiesTable

  class EditorTableCell extends javafx.scene.control.TableCell[Editor, Editor] {
    val wrapper: TableCell[Editor, Editor] = this
    import wrapper._

    def editor: Editor = getItem

    val textField: TextField = new TextField {
      this.onKeyReleased = { (e: scalafx.event.Event) =>
        val event = e.delegate.asInstanceOf[jfxKeyEvent]
        event.getCode match {
          case ENTER =>
            if (editor.isStringEditable) {
              editor.valueString = textField.text.value
              commitEdit(editor)
            } else {
              cancelEdit()
            }

          case ESCAPE =>
            cancelEdit()

          case _ => ()
        }
      }
    }

    override def updateItem(item: Editor, empty: Boolean) {
      super.updateItem(item, empty)

      if (empty) {
        setText(null)
        setGraphic(null)
      } else {
        if (isEditing) {
          textField.text = item.valueString
          setText(null)
          setGraphic(textField)
        } else {
          setText(item.valueString)
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
