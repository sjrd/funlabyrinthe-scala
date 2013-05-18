package com.funlabyrinthe

import scala.language.implicitConversions

import core._

package object mazes {
  implicit def fieldToSquare(field: Field): Square = {
    import field.universe.mazes._
    new Square(field, NoEffect, NoTool, NoObstacle)
  }
}
