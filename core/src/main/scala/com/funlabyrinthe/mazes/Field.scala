package com.funlabyrinthe
package mazes

import scala.language.implicitConversions

import core._

class Field(override implicit val universe: MazeUniverse) extends VisualComponent {
  def entering(context: MoveContext) {}
  def exiting(context: MoveContext) {}

  def entered(context: MoveContext) {}
  def exited(context: MoveContext) {}
}

object Field {
  implicit def fieldToSquare(field: Field): Square = {
    import field.universe.mazes._
    new Square(field, NoEffect, NoTool, NoObstacle)
  }
}
