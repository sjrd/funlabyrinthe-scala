package com.funlabyrinthe.core

import scala.language.{ implicitConversions, higherKinds }

import scala.reflect.ClassTag
import scala.collection.mutable

class Universe {
  // Being myself implicit in subclasses
  protected final implicit def universe: this.type = this

  // Loaders
  lazy val classLoader: ClassLoader = this.getClass().getClassLoader()
  lazy val resourceClassLoader: ClassLoader = classLoader

  // Image loader and painters

  lazy val imageLoader = new graphics.ImageLoader(resourceClassLoader)

  type GraphicsContext = graphics.GraphicsContext
  type DrawContext = graphics.DrawContext
  type Rectangle2D = graphics.Rectangle2D
  type Painter = graphics.Painter

  lazy val EmptyPainter = new Painter(imageLoader)

  // Categories

  private[core] val _categoriesByID =
    new mutable.HashMap[String, ComponentCategory]

  val DefaultCategory = ComponentCategory("default", "Default")

  // Components

  private val _components = new mutable.ArrayBuffer[Component]
  private val _componentsByID = new mutable.HashMap[String, Component]

  def allComponents: IndexedSeq[Component] = _components.toIndexedSeq
  def components[A <: Component : ClassTag]: IndexedSeq[A] = {
    allComponents.collect {
      case c: A => c
    }
  }

  private[core] def componentAdded(component: Component) {
    _components += component
    if (!component.id.isEmpty())
      _componentsByID += component.id -> component
  }

  private[core] def componentIDChanged(component: Component,
      oldID: String, newID: String) {
    _componentsByID -= oldID
    if (!newID.isEmpty())
      _componentsByID += newID -> component
  }

  private[core] def componentIDExists(id: String) = _componentsByID contains id

  def getComponentByID(id: String): Component = {
    _componentsByID(id)
  }

  // Initialization

  def initialize() {
  }
}
