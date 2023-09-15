package com.funlabyrinthe.core

abstract class VisualComponent(using ComponentInit) extends NamedComponent derives Reflector {
  import universe._

  var painter: Painter = EmptyPainter

  override def reflect() = autoReflect[VisualComponent]

  def drawTo(context: DrawContext): Unit = {
    painter.drawTo(context)
  }

  override def drawIcon(context: DrawContext): Unit = {
    drawTo(context)
  }
}
