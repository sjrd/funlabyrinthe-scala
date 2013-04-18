package com.funlabyrinthe.core

trait VisualComponent extends NamedComponent {
  import universe._

  var painter: Painter = EmptyPainter

  def drawTo(context: DrawContext) {
    painter.drawTo(context)
  }
}
