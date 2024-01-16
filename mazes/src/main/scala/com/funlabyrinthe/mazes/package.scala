package com.funlabyrinthe

import scala.language.implicitConversions

import com.funlabyrinthe.core.*

package object mazes {
  implicit def fieldToSquare(field: Field): Square = {
    given Universe = field.universe
    new Square(field, noEffect, noTool, noObstacle)
  }
}
