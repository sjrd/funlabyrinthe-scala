package com.funlabyrinthe.editor.inspector.jfx

import com.funlabyrinthe.core.Component

import com.funlabyrinthe.editor.inspector._

import scala.collection.mutable

import javafx.scene.input.{ KeyEvent => jfxKeyEvent }
import javafx.scene.input.KeyCode._

import scalafx.application.Platform
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

  hbarPolicy = ScrollPane.ScrollBarPolicy.Never
  fitToWidth = true
  styleClass += "inspector"

  private val _inspectedObject = ObjectProperty[Option[Component]](None)
  def inspectedObject = _inspectedObject
  def inspectedObject_=(v: Option[Component]): Unit = {
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

  /* Helper to force inference to choose the scala.Function1 overloads. */
  private def makeCellFactory(f: TableColumn[Descriptor, Descriptor] => TableCell[Descriptor, Descriptor])
      : (TableColumn[Descriptor, Descriptor] => TableCell[Descriptor, Descriptor]) = {
    f
  }

  private val propertiesTable = new TableView(descriptors) {
    minHeight <== Inspector.this.height - 2.0

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
      cellFactory = makeCellFactory({ column =>
        new PropertyNameTableCell
      })
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
      cellFactory = makeCellFactory({ column =>
        new PropertyValueTableCell
      })
    }.delegate

    // Auto-edit property value when the row is selected
    selectionModel.value.selectedIndex.onChange {
      (_, _, index) =>
        if (index.intValue >= 0) {
          Platform.runLater {
            edit(index.intValue, columns(1))
          }
        }
    }
  }

  content = propertiesTable

  class PropertyNameTableCell extends javafx.scene.control.TableCell[Descriptor, Descriptor] {
    val wrapper: TableCell[Descriptor, Descriptor] = this
    import wrapper._

    def editor: Editor = getItem.editor

    val expandButton = new Button {
      text = "+"
      onAction = () => expandCollapse()
      styleClass += "expand-button"
    }
    val label = new Label {
      text = ""
    }
    val content = new HBox {
      spacing = 4.0
      alignment = Pos.BaselineLeft
      children = List(expandButton, label)
    }

    private def expandCollapse(): Unit = {
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

    override def updateItem(item: Descriptor, empty: Boolean): Unit = {
      super.updateItem(item, empty)

      if (empty) {
        setText(null)
        setGraphic(null)
      } else {
        content.padding = Insets(0, 0, 0, 16*item.level)
        expandButton.text =
          if (!item.editor.hasChildren) " "
          else if (item.expanded) "-"
          else "+"
        expandButton.visible = item.editor.hasChildren
        label.text = item.editor.name

        setGraphic(content)
      }
    }
  }

  class PropertyValueTableCell extends javafx.scene.control.TableCell[Descriptor, Descriptor] {
    val wrapper: TableCell[Descriptor, Descriptor] = this
    import wrapper._

    def editor: Editor = getItem.editor
    def currentTextField: TextField =
      if (editor.hasValueList) comboBox.editor.value
      else textField

    val textField: TextField = new TextField {
      onKeyReleased = editKeyReleased(this) _
    }

    val comboBox = new ComboBox[Either[String, Any]] {
      // Left(s) for user input
      // Right(v) for a value selected from the dropdown

      editable = true

      def propEditor = PropertyValueTableCell.this.editor

      this.converter = new scalafx.util.StringConverter[Either[String, Any]] {
        override def fromString(s: String) = Left(s)
        override def toString(v: Either[String, Any]) = v match {
          case Left(s) => s
          case Right(value) => value.toString
        }
      }

      this.editor.value.onKeyReleased = editKeyReleased(this.editor.value) _

      onAction = () => comboBoxValueChanged()

      private def comboBoxValueChanged(): Unit = {
        this.value.value match {
          case Left(s) => ()
          case Right(v) =>
            propEditor.selectValueListItem(v)
            commitEdit(getItem)
        }
      }
    }

    val editButton = new Button {
      text = "..."
      onAction = () => editButtonClicked()
    }

    val content = new HBox {
      spacing = 0
      fillHeight = true
    }

    private def editKeyReleased(source: TextField)(e: scalafx.event.Event): Unit = {
      val event = e.delegate.asInstanceOf[jfxKeyEvent]

      event.getCode match {
        case ENTER =>
          if (editor.isStringEditable && editor.valueString != source.text.value) {
            editor.valueString = source.text.value
            commitEdit(getItem)
          } else {
            cancelEdit()
          }

        case ESCAPE =>
          cancelEdit()

        case UP | KP_UP =>
          propertiesTable.selectionModel.value.selectAboveCell()

        case DOWN | KP_DOWN =>
          propertiesTable.selectionModel.value.selectBelowCell()

        case _ => ()
      }
    }

    private def editButtonClicked(): Unit = {
      assert(editor.hasEditButton)
      editor.clickEditButton()
    }

    override def updateItem(item: Descriptor, empty: Boolean): Unit = {
      super.updateItem(item, empty)

      comboBox.items.value.clear()

      if (empty) {
        setText(null)
        setGraphic(null)
      } else {
        val editor = item.editor

        if (isEditing) {
          if (editor.hasValueList) {
            comboBox.items.value ++= (editor.valueList map (Right(_)))
            comboBox.value = Left(editor.valueString)
            //comboBox.editor.value.text = editor.valueString
          } else {
            textField.text = item.editor.valueString
          }
        } else {
          setText(editor.valueString)
          setGraphic(null)
        }
      }
    }

    override def startEdit(): Unit = {
      super.startEdit()

      val editor = getItem.editor

      comboBox.items.value.clear()

      val (control, edit) = if (editor.hasValueList) {
        comboBox.items.value ++= (editor.valueList map (Right(_)))
        comboBox.value = Left(editor.valueString)
        //comboBox.editor.value.text = editor.valueString
        comboBox.editor.value.editable = editor.isStringEditable
        (comboBox, comboBox.editor.value): (Control, TextField)
      } else {
        textField.text = editor.valueString
        textField.editable = editor.isStringEditable
        (textField, textField): (Control, TextField)
      }

      if (editor.hasEditButton) {
        content.children = List(control, editButton)
      } else {
        content.children = List(control)
      }

      setText(null)
      setGraphic(content)

      scalafx.application.Platform.runLater {
        edit.requestFocus()
        edit.selectAll()
      }
    }

    override def cancelEdit(): Unit = {
      super.cancelEdit()
      comboBox.items.value.clear()
      setText(editor.valueString)
      setGraphic(null)
    }
  }
}
