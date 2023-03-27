package com.funlabyrinthe.editor.inspector

import com.funlabyrinthe.editor.reflect._

import scala.collection.mutable

class Inspector(val registry: InspectorRegistry) {
  def this() = this(new InspectorRegistry)

  private var _inspectedObject: Option[AnyRef] = None
  def inspectedObject = _inspectedObject
  def inspectedObject_=(v: Option[AnyRef]): Unit = {
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

  private def populateDescriptors(instance: AnyRef): Unit = {
    descriptors ++= Utils.reflectingEditorsForProperties(this, instance)
  }
}
