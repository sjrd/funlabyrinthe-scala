package com.funlabyrinthe.mazes

import com.funlabyrinthe.core.*

abstract class Effect(using ComponentInit) extends SquareComponent {
  category = ComponentCategory("effects", "Effects")

  def entered(context: MoveContext): Control[Unit] = doNothing()
  def exited(context: MoveContext): Control[Unit] = doNothing()

  def execute(context: MoveContext): Control[Unit] = doNothing()
}
