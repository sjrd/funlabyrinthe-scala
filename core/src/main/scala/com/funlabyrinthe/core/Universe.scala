package com.funlabyrinthe.core

import scala.language.{ implicitConversions, higherKinds }

import scala.collection.mutable

class Universe extends Components
                  with Maps
                  with Players {
  // Loaders
  lazy val classLoader: ClassLoader = this.getClass().getClassLoader()
  lazy val resourceClassLoader: ClassLoader = classLoader

  // Image loader and painters

  lazy val imageLoader = new graphics.ImageLoader(resourceClassLoader)

  type GraphicsContext = graphics.GraphicsContext
  type DrawContext = graphics.DrawContext
  type Rectangle2D = graphics.Rectangle2D
  type Painter = graphics.Painter

  object EmptyPainter extends Painter(imageLoader)

  implicit def singleNameToPainter(name: String): Painter =
    EmptyPainter + name

  // Universe plugins

  private val _plugins =
    new mutable.HashMap[Class[_], UniversePlugin]

  def plugin[A[U <: Universe] <: UniversePlugin](
      implicit tag: scala.reflect.ClassTag[A[_]]): A[this.type] = {

    val cls = tag.runtimeClass.asInstanceOf[Class[_]]
    _plugins.getOrElseUpdate(cls, {
      cls.getConstructor(classOf[Universe]).newInstance(
          this).asInstanceOf[UniversePlugin]
    }).asInstanceOf[A[this.type]]
  }
}
