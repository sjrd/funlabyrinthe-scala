package com.funlabyrinthe.editor.inspector

import scala.reflect.runtime.{ universe => ru }

import scala.collection.mutable

import scalafx.Includes._
import scalafx.scene.layout._
import scalafx.scene.control._
import scalafx.geometry._

import scalafx.beans.property._
import scalafx.beans.value.ObservableValue
import scalafx.collections.ObservableBuffer

class Inspector extends ScrollPane {
  hbarPolicy = ScrollPane.ScrollBarPolicy.NEVER
  fitToWidth = true

  private val _inspectedObject = ObjectProperty[Option[AnyRef]](None)
  def inspectedObject = _inspectedObject
  def inspectedObject_=(v: Option[AnyRef]) {
    inspectedObject() = v
  }

  val descriptors = new ObservableBuffer[PropertyDescriptor]

  private val propertiesTable = new TableView(descriptors) {
    columns += new TableColumn[PropertyDescriptor, String] {
      text = "Properties"
      cellValueFactory = {
        features =>
          val descriptor = features.value
          new ReadOnlyStringWrapper(descriptor, descriptor.name)
      }
    }.delegate

    columns += new TableColumn[PropertyDescriptor, Any] {
      text = "Values"
      cellValueFactory = {
        features =>
          val descriptor = features.value
          new ReadOnlyObjectWrapper(descriptor, descriptor.valueString)
      }
    }.delegate
  }

  content = propertiesTable

  inspectedObject onChange {
    (_, _, instance) =>
      println(s"Inspect $instance")
      clearDescriptors()
      instance foreach populateDescriptors
  }

  private def clearDescriptors() {
    descriptors.clear()
  }

  private def populateDescriptors(instance: AnyRef) {
    val m = ru.runtimeMirror(instance.getClass.getClassLoader)
    val im = m.reflect(instance)
    val tpe = im.symbol.toType
    for (member <- tpe.members) {
      if (member.isPublic && member.isTerm && member.asTerm.isGetter) {
        println(member)
        val method = im.reflectMethod(member.asMethod)
        val descriptor = new ReadOnlyPropertyDescriptor(method)
        println((descriptor.name, descriptor.valueString))
        descriptors += descriptor
      }
    }
  }
}
