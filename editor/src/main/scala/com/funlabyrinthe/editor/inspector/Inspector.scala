package com.funlabyrinthe.editor.inspector

import scala.collection.mutable

import com.funlabyrinthe.core.Component

class Inspector {
  private var _inspectedObject: Option[Component] = None
  def inspectedObject = _inspectedObject
  def inspectedObject_=(v: Option[Component]): Unit = {
    _inspectedObject = v
    clearDescriptors()
    v foreach populateDescriptors
    onChange()
  }

  private var _onChange: () => Unit = () => ()
  def onChange = _onChange
  def onChange_=(body: => Unit): Unit = _onChange = () => body

  val descriptors = new mutable.ArrayBuffer[Editor]

  private def clearDescriptors(): Unit = {
    descriptors.clear()
  }

  private def populateDescriptors(instance: Component): Unit = {
    descriptors ++= Utils.reflectingEditorsForProperties(this, instance)(using instance.universe)
  }
}
