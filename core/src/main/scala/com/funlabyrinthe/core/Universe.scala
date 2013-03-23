package com.funlabyrinthe.core

import scala.language.implicitConversions

abstract class Universe extends Components
                           with Maps
                           with Players {
  lazy val classLoader: ClassLoader = this.getClass().getClassLoader()
  lazy val resourceClassLoader: ClassLoader = classLoader

  lazy val imageLoader = new graphics.ImageLoader(resourceClassLoader)

  type Painter = graphics.Painter

  object EmptyPainter extends Painter(imageLoader)

  implicit def singleNameToPainter(name: String) =
    EmptyPainter + name
}
