package com.funlabyrinthe.core

trait VisualComponent extends NamedComponent {
  import universe._

  var painter: Painter = EmptyPainter

  def drawTo(context: DrawContext): Unit = {
    painter.drawTo(context)
  }

  override def drawIcon(context: DrawContext): Unit = {
    drawTo(context)
  }
}
