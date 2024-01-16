package com.funlabyrinthe.mazes

import com.funlabyrinthe.core.*

abstract class Field(using ComponentInit) extends SquareComponent {
  category = ComponentCategory("fields", "Fields")

  def entering(context: MoveContext): Control[Unit] = doNothing()
  def exiting(context: MoveContext): Control[Unit] = doNothing()

  def entered(context: MoveContext): Control[Unit] = doNothing()
  def exited(context: MoveContext): Control[Unit] = doNothing()
}
