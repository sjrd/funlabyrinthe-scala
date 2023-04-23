package com.funlabyrinthe

import scala.language.implicitConversions

import core._

import com.funlabyrinthe.mazes.Mazes.mazes

package object mazes {
  implicit def fieldToSquare(field: Field): Square = {
    given Universe = field.universe
    new Square(field, mazes.NoEffect, mazes.NoTool, mazes.NoObstacle)
  }
}
